import java.io.FileInputStream
import java.nio.file.Paths

class LoadingTools {
    companion object {
        fun getFullResourcePath(resourcePath: String): String {
            val res = this::class.java.classLoader.getResource(resourcePath)
            val file = Paths.get(res.toURI()).toFile()
            return file.absolutePath
        }

        fun readLines(pathToResource: String): List<String> {
            val fullPath = getFullResourcePath(pathToResource)
            val reader = FileInputStream(fullPath).bufferedReader()
            return reader.readLines()
        }
    }
}