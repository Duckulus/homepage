package us.duckul.homepage.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.util.*

val ffmpeg = FFmpeg("ffmpeg")
val ffprobe = FFprobe("ffprobe")

fun Application.configureRouting() {
    routing {
        staticFiles("/", File("./static"))
        staticFiles("/files", File("./files"))
        get("/hello") {
            call.respondText("Hello, world!")
        }
        get("/ip") {
            call.respondText(call.request.headers["cf-connecting-ip"] ?: call.request.local.remoteAddress)
        }
        get("/time") {
            call.respondText(System.currentTimeMillis().toString())
        }
        get("/garfield.mp4") {
            val videoID = UUID.randomUUID()
            val videoName = "./garfield/$videoID.mp4"
            val ip = call.request.headers["cf-connecting-ip"] ?: call.request.local.remoteAddress
            val location =
                "${call.request.headers["cf-ipcity"]},${call.request.headers["cf-region"]},${call.request.headers["cf-ipcountry"]}"
                    .replace(" ", "-")


            val ffmpegBuilder = FFmpegBuilder()
                .setInput("./garfield/garfield.mp4")
                .overrideOutputFiles(true)
                .addOutput(videoName)
                .setAudioCodec("aac")
                .setVideoFilter(
                    "drawtext=fontfile=./garfield/gilligan.ttf:text='${
                        ip.replace(
                            ":",
                            "\\:"
                        )
                    }':fontcolor=yellow:fontsize=96:x=(1150-(text_w/2)):y=600:enable='gte(t,3.75)'," +
                            "drawtext=fontfile=./garfield/gilligan.ttf:text='$location':fontcolor=yellow:fontsize=64:x=(1150-(text_w/2)):y=700:enable='gte(t,3.75)'"
                )
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done()

            val executor = FFmpegExecutor(ffmpeg, ffprobe)
            executor.createJob(ffmpegBuilder).run()

            call.respondFile(File("./garfield/$videoID.mp4"))
            File(videoName).delete()
        }
    }
    install(IgnoreTrailingSlash) {
        routing {
            staticFiles("/", File("./static")) {
                extensions("html", "htm")
            }
        }
    }
    install(StatusPages) {
        statusFile(HttpStatusCode.NotFound, filePattern = "error#.html")
    }
}
