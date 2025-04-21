package es.elhaso.quarrel_parser

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform