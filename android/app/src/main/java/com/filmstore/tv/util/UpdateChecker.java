package com.filmstore.tv.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.leanback.app.ErrorSupportFragment;

import com.filmstore.tv.BuildConfig;
import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.UpdateInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 更新检查器
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";

    private Context context;
    private ApiClient apiClient;
    private Handler mainHandler;
    private boolean isChecking = false;

    /**
     * 更新回调
     */
    public interface UpdateCallback {
        void onUpdateAvailable(UpdateInfo info);
        void onNoUpdate();
        void onError(String message);
    }

    public UpdateChecker(Context context) {
        this.context = context;
        this.apiClient = FilmStoreApp.getInstance().getApiClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 检查更新
     */
    public void checkUpdate(final UpdateCallback callback) {
        if (isChecking) {
            return;
        }
        isChecking = true;

        int currentVersion = BuildConfig.VERSION_CODE;

        apiClient.checkUpdate(currentVersion, new ApiClient.ApiCallback<UpdateInfo>() {
            @Override
            public void onSuccess(UpdateInfo result) {
                isChecking = false;
                if (result != null && result.hasUpdate() && result.getLatestVersion() != null) {
                    mainHandler.post(() -> callback.onUpdateAvailable(result));
                } else {
                    mainHandler.post(() -> callback.onNoUpdate());
                }
            }

            @Override
            public void onError(Exception e) {
                isChecking = false;
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * 显示更新对话框
     */
    public void showUpdateDialog(UpdateInfo updateInfo) {
        if (updateInfo == null || !updateInfo.hasUpdate() || updateInfo.getLatestVersion() == null) {
            return;
        }

        UpdateInfo.VersionInfo version = updateInfo.getLatestVersion();
        boolean isForce = version.isForceUpdate();

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.update_title) + " v" + version.getVersionName())
                .setMessage(formatUpdateLog(version.getUpdateLog()))
                .setCancelable(!isForce);

        if (isForce) {
            builder.setPositiveButton(context.getString(R.string.update_force), (dialog, which) -> {
                startDownload(version);
            });
        } else {
            builder.setPositiveButton(context.getString(R.string.update_download), (dialog, which) -> {
                startDownload(version);
            });
            builder.setNegativeButton(context.getString(R.string.update_later), null);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 格式化更新日志
     */
    private String formatUpdateLog(String log) {
        if (log == null || log.isEmpty()) {
            return "请查看更新说明";
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = log.split("\n");
        for (String line : lines) {
            sb.append("• ").append(line.trim()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 开始下载 APK
     */
    private void startDownload(UpdateInfo.VersionInfo version) {
        String apkUrl = version.getFullApkUrl();
        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(context, "下载地址无效", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示下载进度 Toast
        Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show();

        // 启动下载线程
        FilmStoreApp.getInstance().getExecutorService().execute(() -> {
            try {
                downloadApk(apkUrl, version.getVersionName());
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 下载 APK 到缓存目录并提示安装
     */
    private void downloadApk(String url, String versionName) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("下载失败: " + response.code());
        }

        // 保存到缓存目录
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        File apkFile = new File(cacheDir, "FilmStore_v" + versionName + ".apk");

        try (InputStream is = response.body().byteStream();
             FileOutputStream fos = new FileOutputStream(apkFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = response.body().contentLength();
            long downloadedBytes = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                // 更新进度
                if (totalBytes > 0) {
                    final int progress = (int) (downloadedBytes * 100 / totalBytes);
                    mainHandler.post(() -> {
                        Toast.makeText(context,
                                String.format(context.getString(R.string.update_downloading), progress),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }

        // 提示安装
        mainHandler.post(() -> {
            installApk(apkFile);
        });
    }

    /**
     * 安装 APK
     */
    private void installApk(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法启动安装器: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
