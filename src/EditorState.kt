import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import java.io.File

object State {

    val fileP = SimpleObjectProperty<File?>(null)

    var file: File?
        get() = fileP.get()
        set(value) = fileP.set(value)

    val filenameP = Bindings.createStringBinding(callable
    {if (file == null) Config["default_file_name"] else file!!.name}, fileP)

    val filename: String
    get() = filenameP.get()

    val changedP = SimpleBooleanProperty(false)

    var changed: Boolean
        get() = changedP.get()
        set(value) = changedP.set(value)

}