package com.ivieleague.wardtextsender

import com.lightningkite.kotlin.stream.toString
import java.io.InputStream

/**
 * Created by josep on 5/7/2017.
 */
object ContactFile {
    fun parse(inputStream: InputStream): List<Contact> = parse(inputStream.toString(Charsets.UTF_8))
    fun parse(str: String): List<Contact> {
        val rows = str.split('\n')
        return rows.mapNotNull { row ->
            val columns = row.split(',')
            var totalName = ""
            val tags = ArrayList<String>()
            var phone: String? = null
            for (column in columns) {
                val data = column.trimQuotes()
                val first = data.firstOrNull()
                when (first) {
                    null -> {/*skip*/
                    }
                    in '0'..'9' -> {
                        if (phone == null) {
                            val potentialNumber = data.toPhoneNumber()
                            if (potentialNumber != null)
                                phone = potentialNumber
                        }
                    }
                    '+' -> {
                        if (phone == null) {
                            val potentialNumber = data.toPhoneNumber()
                            if (potentialNumber != null)
                                phone = potentialNumber
                        }
                    }
                    in 'a'..'z' -> {
                        tags += data.split(' ').map { it.trimQuotes() }.filter { it.isNotBlank() }
                    }
                    in 'A'..'Z' -> {
                        totalName += " " + data
                    }
                    else -> {/*skip*/
                    }
                }
            }
            println("$row -> name: $totalName, phone: $phone, tags: $tags")
            if (totalName.isNotBlank() && phone != null) {
                Contact(totalName, phone, tags)
            } else null
        }
    }

    fun write(contacts: List<Contact>): ByteArray = contacts.joinToString("\n") { it.run { "$name, $phoneNumber, ${tags.joinToString()}" } }.toByteArray()

    private fun String.toPhoneNumber(): String? {
        val potentialNumber = this.filter { it.isDigit() }
        val firstDigit = potentialNumber.firstOrNull()
        when (potentialNumber.length) {
            7 -> if (firstDigit != '1') return potentialNumber
            10 -> if (firstDigit != '1') return potentialNumber
            11 -> if (firstDigit == '1') return potentialNumber
        }
        return null
    }

    private fun String.trimQuotes(): String = let { if (it.startsWith('"')) it.drop(1) else it }.let { if (it.endsWith('"')) it.dropLast(1) else it }.trim()
}