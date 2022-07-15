package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.system.exitProcess

data class TColor(val usingTColor: Boolean, val color: Color)

data class WatermarkPos(val single: Boolean, val pos: Pair<Int, Int>)

fun inputImage(type: String): BufferedImage {
    println("Input the ${if (type != "image") "$type " else ""}image filename:")
    val filename = readln()
    if (!File(filename).exists()) {
        println("The file $filename doesn't exist.")
        exitProcess(21)
    }
    val image = ImageIO.read(File(filename))
    if (image.colorModel.numColorComponents != 3) {
        println("The number of $type color components isn't 3.")
        exitProcess(22)
    }
    if (image.colorModel.pixelSize !in listOf(24, 32)) {
        println("The $type isn't 24 or 32-bit.")
        exitProcess(23)
    }
    return image
}

fun dimensionCheck(image: BufferedImage, watermark: BufferedImage) {
    if (image.width < watermark.width || image.height < watermark.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(24)
    }
}

fun checkAlpha(watermark: BufferedImage): Boolean {
    if (watermark.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        if (readln().lowercase() == "yes") {
            return true
        }
    }
    return false
}

fun getTColor(image: BufferedImage): TColor {
    if (image.transparency == Transparency.TRANSLUCENT) {
        return TColor(false, Color(0))
    }
    println("Do you want to set a transparency color?")
    if (readln().lowercase() != "yes") {
        return TColor(false, Color(0))
    }
    println("Input a transparency color ([Red] [Green] [Blue]):")
    val colors = readln().split(" ")
    if (colors.size != 3) {
        println("The transparency color input is invalid.")
        exitProcess(28)
    }
    for (colorString in colors) {
        if (!Regex("\\d+").matches(colorString) || colorString.toInt() !in 0..255) {
            println("The transparency color input is invalid.")
            exitProcess(28)
        }
    }
    return TColor(true, Color(colors[0].toInt(), colors[1].toInt(), colors[2].toInt()))
}

fun inputTransparency(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    val transparency = readln()
    if (!Regex("\\d+").matches(transparency)) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(25)
    }
    val trans = transparency.toInt()
    if (trans !in 0..100) {
        println("The transparency percentage is out of range.")
        exitProcess(26)
    }
    return trans
}

fun setPos(im: BufferedImage, wm: BufferedImage): Pair<Int, Int> {
    println("Input the watermark position ([x 0-${im.width - wm.width}] [y 0-${im.height - wm.height}]):")
    val pos = readln().split(" ")
    if (pos.size != 2 || !Regex("[+-]?\\d+").matches(pos[0]) || !Regex("[+-]?\\d+").matches(pos[1])) {
        println("The position input is invalid.")
        exitProcess(30)
    }
    if (pos[0].toInt() !in 0..(im.width - wm.width) || pos[1].toInt() !in 0..(im.height - wm.height)) {
        println("The position input is out of range.")
        exitProcess(31)
    }
    return Pair(pos[0].toInt(), pos[1].toInt())
}

fun getWatermarkPos(image: BufferedImage, watermark: BufferedImage): WatermarkPos {
    println("Choose the position method (single, grid):")
    when (readln()) {
        "single" -> return WatermarkPos(true, setPos(image, watermark))
        "grid" -> return WatermarkPos(false, Pair(0, 0))
        else -> {
            println("The position method input is invalid.")
            exitProcess(29)
        }
    }
}

fun placeWatermark(im: BufferedImage, wm: BufferedImage, weight: Int, uA: Boolean, tColor: TColor, xPos: Int, yPos: Int) {
    for (x in xPos until min(xPos + wm.width, im.width)) {
        for (y in yPos until min(yPos + wm.height, im.height)) {
            val i = Color(im.getRGB(x, y))
            val w = if (uA) Color(wm.getRGB(x - xPos, y - yPos), true) else Color(wm.getRGB(x - xPos, y - yPos))
            if (!(uA && w.alpha == 0 || tColor.usingTColor && tColor.color == Color(wm.getRGB(x - xPos, y - yPos)))) {
                val color = Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
                im.setRGB(x, y, color.rgb)
            }
        }
    }
}

fun watermarkImage(im: BufferedImage, wm: BufferedImage, weight: Int, uA: Boolean, tColor: TColor, wPos: WatermarkPos): BufferedImage {
    if (wPos.single) {
        placeWatermark(im, wm, weight, uA, tColor, wPos.pos.first, wPos.pos.second)
    }
    else {
        for (xPos in 0 until im.width step wm.width) {
            for (yPos in 0 until im.height step wm.height) {
                placeWatermark(im, wm, weight, uA, tColor, xPos, yPos)
            }
        }
    }
    println()
    return im
}

fun saveImage(image: BufferedImage) {
    println("Input the output image filename (jpg or png extension):")
    val filename = readln()
    val fileType = filename.takeLastWhile { it != '.' }
    if (!filename.contains('.') || fileType !in listOf("jpg", "png")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(27)
    }
    ImageIO.write(image, fileType, File(filename))
    println("The watermarked image $filename has been created.")
}

fun main() {
    val image = inputImage("image")
    val watermark = inputImage("watermark")
    dimensionCheck(image, watermark)
    val usingAlpha = checkAlpha(watermark)
    val tColor = getTColor(watermark)
    val trans = inputTransparency()
    val wPos = getWatermarkPos(image, watermark)
    saveImage(watermarkImage(image, watermark, trans, usingAlpha, tColor, wPos))
}