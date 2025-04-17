package com.example.blastjargas.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Status(
    var status: String? = null,
    var semua: Int? = null
):Parcelable