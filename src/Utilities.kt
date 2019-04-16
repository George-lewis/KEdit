import javafx.scene.control.Hyperlink
import javafx.scene.text.Font
import javafx.util.Callback
import java.util.concurrent.Callable

// ### Constants object ###

object Constants {

    val Version = "Beta"

}

// ### Font stuff ###

fun Font.size(size: Double): Font = Font(this.name, size);

fun Font.enlarge(change: Double): Font = this.size(this.size + change)

fun Font.shrink(change: Double): Font = this.size(this.size - change)

// ### Mutable pairs ###

data class MPair <T,V> (var first: T, var second: V)

infix fun <T,V> T.mto(v: V) = MPair(this, v)

// ### Misc ###

fun systemOpen(link: String) = app.hostServices.showDocument(link)

fun hyperlink(text: String, link: String) = Hyperlink(text).apply { setOnAction { systemOpen(link) } }

// Counts the number of words in a string
fun String.numWords() = this.split(" ", "\t", "\n").filter(String::isNotEmpty).size