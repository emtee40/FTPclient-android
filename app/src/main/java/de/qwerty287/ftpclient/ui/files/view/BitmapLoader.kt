package de.qwerty287.ftpclient.ui.files.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView

internal object BitmapLoader {
    class LoadException : Exception()

    fun load(byteList: ArrayList<Int>, iv: ImageView) {
        val byteArray = ByteArray(byteList.size)
        for (i in 0 until byteList.size) {
            byteArray[i] = byteList[i].toByte()
        }


        val bmp =
            decodeSampledBitmapFromResource(byteArray, byteList.size, iv.width, iv.height) ?: throw LoadException()
        iv.setImageBitmap(bmp)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun decodeSampledBitmapFromResource(
        arr: ByteArray,
        size: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(arr, 0, size, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(arr, 0, size, this)
        }
    }


}