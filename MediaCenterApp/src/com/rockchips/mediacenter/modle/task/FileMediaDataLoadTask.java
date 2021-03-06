package com.rockchips.mediacenter.modle.task;
import java.io.File;
import java.util.UUID;
import momo.cn.edu.fjnu.androidutils.data.CommonValues;
import momo.cn.edu.fjnu.androidutils.utils.BitmapUtils;
import momo.cn.edu.fjnu.androidutils.utils.SizeUtils;

import com.rockchips.mediacenter.bean.LocalMediaFile;
import com.rockchips.mediacenter.data.ConstData;
import com.rockchips.mediacenter.modle.db.LocalMediaFileService;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;

/**
 * @author GaoFei
 *
 */
public class FileMediaDataLoadTask extends AsyncTask<LocalMediaFile, Integer, Integer>{
	public static final String TAG = FileMediaDataLoadTask.class.getSimpleName();
	public interface CallBack{
		void onFinish(LocalMediaFile localMediaFile);
	}
	private CallBack mCallBack;
	private LocalMediaFile mLocalMediaFile;
	private boolean isOOM;
	public FileMediaDataLoadTask(CallBack callBack){
		mCallBack = callBack;
	}
	
	@Override
	protected Integer doInBackground(LocalMediaFile... params) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		Log.i(TAG, "doInBackground");
		/**
		 * 媒体信息元数据获取器
		 * */
		MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
		LocalMediaFile localMediaFile = params[0];
		mLocalMediaFile = localMediaFile;
		try{
			//此处发生异常，直接导致文件元数据无法解析
			mediaMetadataRetriever.setDataSource(localMediaFile.getPath());
		}catch (Exception e){
			//存在发生异常的可能性
			Log.e(TAG, "doInBackground->setDataSource->exception:" + e);
		}
		
		String durationStr = null;
		try{
			 durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		}catch (Exception e){
			//存在发生异常的可能性
			Log.e(TAG, "doInBackground->extractMetadata->exception:" + e);
		}
		
		if(durationStr != null){
			localMediaFile.setDuration(getDuration(Long.parseLong(durationStr)));
		}
		Bitmap priviewBitmap = null;
		if(localMediaFile.getType() == ConstData.MediaType.VIDEO){
			priviewBitmap = ThumbnailUtils.createVideoThumbnail(localMediaFile.getPath(), Thumbnails.MICRO_KIND);
		}else{
			byte[] albumData = mediaMetadataRetriever.getEmbeddedPicture();
			if(albumData != null && albumData.length > 0){
				BitmapFactory.Options options = new BitmapFactory.Options();
				int targetWidth  = SizeUtils.dp2px(CommonValues.application, 280);
				int targetHeight = SizeUtils.dp2px(CommonValues.application, 280);
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeByteArray(albumData, 0, albumData.length, options);
				int bitmapWidth = options.outWidth;
				int bitmapHeight = options.outHeight;
				Log.i(TAG, "doInBackground->bitmapWidth:" + bitmapWidth);
				Log.i(TAG, "doInBackground->bitmapHeight:" + bitmapHeight);
				options.inJustDecodeBounds = false;
				int scaleX = bitmapWidth / targetWidth;
				int scaleY = bitmapHeight / targetHeight;
				int scale = Math.max(scaleX, scaleY);
				if(scale > 1)
					options.inSampleSize = scale;
				else
					options.inSampleSize = 1;
				try{
					priviewBitmap = BitmapFactory.decodeByteArray(albumData, 0, albumData.length, options);
				}catch (OutOfMemoryError error){
					isOOM = true;
				}
				
			}
		}
		File cacheImageDirFile = new File(ConstData.CACHE_IMAGE_DIRECTORY);
		if(!cacheImageDirFile.exists())
			cacheImageDirFile.mkdirs();
		String savePath = cacheImageDirFile.getPath() + "/" + UUID.randomUUID().toString() + ".png";
		if(priviewBitmap != null && BitmapUtils.saveBitmapToImage(priviewBitmap, savePath, CompressFormat.PNG, 80))
			localMediaFile.setPreviewPhotoPath(savePath);
		//未发生OOM异常
		if(!isOOM)
			localMediaFile.setLoadPreviewPhoto(true);
		//更新至数据库中
		LocalMediaFileService localMediaFileService = new LocalMediaFileService();
		localMediaFileService.update(localMediaFile);
		return ConstData.TaskExecuteResult.SUCCESS;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		mCallBack.onFinish(mLocalMediaFile);
	}
	
	
	public String getDuration(long time){
		String duration = null;
		long secondes = time / 1000;
		long hour = secondes / 60 / 60;
		long minute = secondes / 60 % 60;
		long second = secondes % 60;
		duration = String.format("%02d:%02d:%02d", hour, minute, second);
		return duration;
	}
}
