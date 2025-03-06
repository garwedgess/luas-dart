package com.wedgess.luas.data

import java.io.File

internal fun loadXML(fileName: String): String {
    return File("src/test/resources/xml/$fileName").readText(Charsets.UTF_8)
}
