package com.footballai.analyzer.util

data class AiSection(
    val title: String,
    val body: String
)

object AiSectionParser {
    /**
     * ## Cím\n szöveg  → szekciók. Ha nincs ##, egy "Elemzés" blokk.
     */
    fun parse(text: String): List<AiSection> {
        if (text.isBlank()) return emptyList()
        val lines = text.lines()
        val sections = mutableListOf<AiSection>()
        var title: String? = null
        val body = StringBuilder()

        fun flush() {
            val t = title
            if (t != null) {
                sections.add(AiSection(t, body.toString().trim()))
            } else if (body.isNotBlank()) {
                sections.add(AiSection("Elemzés", body.toString().trim()))
            }
            body.clear()
        }

        for (raw in lines) {
            val line = raw.trimEnd()
            val header = Regex("^#{1,3}\\s+(.+)$").find(line.trim())
                ?: Regex("^(\\d+)[.)]\\s+(.+)$").find(line.trim())
            if (header != null) {
                flush()
                title = header.groupValues.last().trim()
            } else {
                if (body.isNotEmpty()) body.append('\n')
                body.append(line)
            }
        }
        flush()
        return sections.filter { it.body.isNotBlank() || it.title.isNotBlank() }
    }
}
