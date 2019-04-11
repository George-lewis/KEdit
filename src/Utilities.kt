import javafx.scene.text.Font
import java.util.concurrent.Callable

fun Font.size(size: Double): Font = Font(this.name, size);

fun Font.enlarge(change: Double): Font = this.size(this.size + change)

fun Font.shrink(change: Double): Font = this.size(this.size - change)

fun runnable(f: () -> Unit): Runnable = object : Runnable {override fun run() = f()}

fun <T> callable(f: () -> T) = Callable<T> { return@Callable f() }