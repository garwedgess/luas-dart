package com.wedgess.verification

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/** Checks the actual packaged 64-bit binaries, including transitive SDKs. 32-bit ELF alignment is not a 16KB requirement. */
@DisableCachingByDefault(because = "Verification has no outputs")
abstract class Verify16KbNativeLibraries : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apks: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val failures = mutableListOf<String>()
        apks.files.sorted().forEach { apk ->
            var checked = 0
            ZipFile(apk).use { zip ->
                zip.entries().asSequence().filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
                    .forEach { entry ->
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        check(bytes.size >= ELF_HEADER_SIZE && bytes[0] == ELF_MAGIC &&
                            bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
                            "Invalid ELF: ${apk.name}/${entry.name}"
                        }
                        if (bytes[ELF_CLASS_OFFSET].toInt() == ELF_CLASS_64) {
                            checked++
                            if (!isAligned(bytes)) failures += "${apk.name}/${entry.name}"
                        }
                    }
            }
            check(checked > 0) { "No 64-bit native libraries found in ${apk.name}" }
            logger.lifecycle("Checked {} 64-bit native libraries in {}", checked, apk.name)
        }
        if (failures.isNotEmpty()) {
            throw GradleException("Native libraries lack 16KB ELF alignment:\n${failures.joinToString("\n")}")
        }
    }

    private fun isAligned(bytes: ByteArray): Boolean {
        val order = if (bytes[ELF_ENDIAN_OFFSET].toInt() == LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val buffer = ByteBuffer.wrap(bytes).order(order)
        val table = buffer.getLong(PROGRAM_TABLE_OFFSET).toInt()
        val size = buffer.getShort(PROGRAM_ENTRY_SIZE_OFFSET).toInt() and UNSIGNED_SHORT_MASK
        val count = buffer.getShort(PROGRAM_COUNT_OFFSET).toInt() and UNSIGNED_SHORT_MASK
        var loads = 0
        repeat(count) { index ->
            val start = table + index * size
            if (buffer.getInt(start) == PT_LOAD) {
                loads++
                val offset = buffer.getLong(start + LOAD_FILE_OFFSET)
                val address = buffer.getLong(start + LOAD_ADDRESS_OFFSET)
                val alignment = buffer.getLong(start + LOAD_ALIGNMENT_OFFSET)
                if (alignment < PAGE_BYTES || offset % PAGE_BYTES != address % PAGE_BYTES) return false
            }
        }
        return loads > 0
    }

    private companion object {
        const val ELF_HEADER_SIZE = 64
        const val ELF_MAGIC: Byte = 0x7f
        const val ELF_CLASS_OFFSET = 4
        const val ELF_CLASS_64 = 2
        const val ELF_ENDIAN_OFFSET = 5
        const val LITTLE_ENDIAN = 1
        const val PROGRAM_TABLE_OFFSET = 32
        const val PROGRAM_ENTRY_SIZE_OFFSET = 54
        const val PROGRAM_COUNT_OFFSET = 56
        const val UNSIGNED_SHORT_MASK = 0xffff
        const val PT_LOAD = 1
        const val LOAD_FILE_OFFSET = 8
        const val LOAD_ADDRESS_OFFSET = 16
        const val LOAD_ALIGNMENT_OFFSET = 48
        const val PAGE_BYTES = 16_384L
    }
}
