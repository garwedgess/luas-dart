package com.wedgess.luas.data.timetable

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun fixtureZip(file: File, overrides: Map<String, String?> = emptyMap()): File {
    val tables = mutableMapOf<String, String?>(
        "agency.txt" to "agency_id,agency_timezone\nIR,Europe/Dublin\n",
        "routes.txt" to "route_id,route_short_name,route_type\nr,DART,2\nx,InterCity,2\nl,Green,0\n",
        "trips.txt" to """
            route_id,service_id,trip_id,trip_headsign,direction_id
            r,weekday,t1,Bray (Daly),1
            r,special,t2,Howth,0
            x,weekday,other,Cork,1
            l,weekday,tram,Brides Glen,0
        """.trimIndent(),
        "stops.txt" to """
            stop_id,stop_name
            c,Connolly
            b,Bray (Daly)
            x,Cork
            a1,Abbey St.
            a2,Abbey St.
        """.trimIndent(),
        "calendar.txt" to """
            service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
            weekday,1,1,1,1,1,0,0,20261109,20261113
            special,0,0,0,0,0,0,0,20261110,20261110
        """.trimIndent(),
        "calendar_dates.txt" to "service_id,date,exception_type\nspecial,20261110,1\n",
        "stop_times.txt" to """
            trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type
            t1,23:50:00,23:50:00,c,1,,0
            t1,24:10:00,24:10:00,b,2,,1
            t2,25:10:00,25:10:00,b,1,,0
            t2,25:50:00,25:50:00,c,2,,1
            other,12:00:00,12:00:00,x,1,,0
            tram,12:00:00,12:00:00,a1,1,,0
            tram,12:02:00,12:02:00,a2,2,,0
        """.trimIndent(),
        "feed_info.txt" to "feed_version,feed_end_date\nfixture,20271108\n"
    )
    tables.putAll(overrides)
    ZipOutputStream(file.outputStream()).use { zip ->
        tables.forEach { (name, data) ->
            if (data != null) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
    return file
}
