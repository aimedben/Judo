package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun generateAttendanceCsv(
        attendances: List<AttendanceWithMember>,
        title: String = "presences_judo_seddouk"
    ): String {
        val sb = StringBuilder()
        sb.append("Date,Heure,Nom,Prénom,Groupe,Ceinture,Code_QR,Telephone\n")
        attendances.forEach { item ->
            sb.append("\"${item.dateString}\",")
            sb.append("\"${item.timeString}\",")
            sb.append("\"${item.lastName}\",")
            sb.append("\"${item.firstName}\",")
            sb.append("\"${item.groupCode}\",")
            sb.append("\"${item.beltName}\",")
            sb.append("\"${item.qrCode}\",")
            sb.append("\"${item.phone}\"\n")
        }
        return sb.toString()
    }

    fun shareAttendanceReport(
        context: Context,
        attendances: List<AttendanceWithMember>,
        filterLabel: String
    ) {
        val csvData = generateAttendanceCsv(attendances)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pointage Judo Seddouk - $filterLabel")
            putExtra(
                Intent.EXTRA_TEXT,
                "🥋 Rapport de présence - Judo Seddouk ($filterLabel)\n" +
                "Total pointages: ${attendances.size}\n\n" +
                csvData
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager le rapport de présence"))
    }
}
