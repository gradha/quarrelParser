package es.elhaso.quarrelParser

import es.elhaso.quarrelParser.shared.V_MAJOR
import es.elhaso.quarrelParser.shared.V_MINOR
import es.elhaso.quarrelParser.shared.V_PATCH

class QuarrelParser {

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
    )

    companion object {
        val version: Version = Version(
            major = V_MAJOR,
            minor = V_MINOR,
            patch = V_PATCH
        )
    }
}