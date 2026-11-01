package com.agrodoc.app.data.db

import androidx.room.TypeConverter
import com.agrodoc.app.domain.model.TrustLevel

/** Room type converters for non-primitive fields. */
class Converters {

    @TypeConverter
    fun trustToString(trust: TrustLevel): String = trust.name

    @TypeConverter
    fun stringToTrust(value: String): TrustLevel = TrustLevel.valueOf(value)
}
