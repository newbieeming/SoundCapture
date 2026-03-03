package com.xmbest.soundcapture.data.model

import android.media.AudioFormat

/**
 * AudioFormat hide的变量
 * @see AudioFormat
 */
class AudioFormatExt {
    companion object {
        const val CHANNEL_IN_BACK_LEFT = 0x10000
        const val CHANNEL_IN_BACK_RIGHT: Int = 0x20000
        const val CHANNEL_IN_CENTER: Int = 0x40000
        const val CHANNEL_IN_LOW_FREQUENCY: Int = 0x100000
        const val CHANNEL_IN_TOP_LEFT = 0x200000
        const val CHANNEL_IN_TOP_RIGHT = 0x400000
        const val CHANNEL_IN_2POINT0POINT2 = (
                AudioFormat.CHANNEL_IN_LEFT or AudioFormat.CHANNEL_IN_RIGHT or CHANNEL_IN_TOP_LEFT or CHANNEL_IN_TOP_RIGHT)
        const val CHANNEL_IN_2POINT1POINT2 =
            (AudioFormat.CHANNEL_IN_LEFT or AudioFormat.CHANNEL_IN_RIGHT or CHANNEL_IN_TOP_LEFT or CHANNEL_IN_TOP_RIGHT
                    or CHANNEL_IN_LOW_FREQUENCY)
        const val CHANNEL_IN_3POINT0POINT2 =
            (AudioFormat.CHANNEL_IN_LEFT or CHANNEL_IN_CENTER or AudioFormat.CHANNEL_IN_RIGHT or CHANNEL_IN_TOP_LEFT
                    or CHANNEL_IN_TOP_RIGHT)
        const val CHANNEL_IN_3POINT1POINT2 =
            (AudioFormat.CHANNEL_IN_LEFT or CHANNEL_IN_CENTER or AudioFormat.CHANNEL_IN_RIGHT or CHANNEL_IN_TOP_LEFT
                    or CHANNEL_IN_TOP_RIGHT or CHANNEL_IN_LOW_FREQUENCY)
        const val CHANNEL_IN_5POINT1 =
            (AudioFormat.CHANNEL_IN_LEFT or CHANNEL_IN_CENTER or AudioFormat.CHANNEL_IN_RIGHT or CHANNEL_IN_BACK_LEFT
                    or CHANNEL_IN_BACK_RIGHT or CHANNEL_IN_LOW_FREQUENCY)
        const val CHANNEL_IN_FRONT_BACK =
            AudioFormat.CHANNEL_IN_FRONT or AudioFormat.CHANNEL_IN_BACK
    }
}