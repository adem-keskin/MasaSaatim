package com.masasaatim.data.model

import com.google.gson.annotations.SerializedName

data class PrayerResponseDto(
    @SerializedName("data") val dataList: List<PrayerDayDto>
)

data class PrayerDayDto(
    @SerializedName("timings") val timings: TimingsDto,
    @SerializedName("date") val dateInfo: DateInfoDto
)

data class TimingsDto(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String
)

data class DateInfoDto(
    @SerializedName("gregorian") val gregorian: GregorianDto
)

data class GregorianDto(
    @SerializedName("date") val readableDate: String
)
