package com.example.blastjargas.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Tunggakan (
    var idTunggakan: String? = null,
    var noMeter: String? = null,
    var sectorTunggakan: String? = null,
    var namaTunggakan: String? = null,
    var alamatTunggakan: String? = null,
    var phoneTunggakan: String? = null,
    var tipeTunggakan: String? = null,
    var tagihanTunggakan: Int? = null,
    var biayaTunggakan: Int? = null,
    var rincianTunggakan: String? = null,
    var returnPPnTunggakan: Int? = null,
    var dendaTunggakan: Int? = null,
    var jumlahTunggakan: Int? = null,
    var selesai: String? = null
):Parcelable