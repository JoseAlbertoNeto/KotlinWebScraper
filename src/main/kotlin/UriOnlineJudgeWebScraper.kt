import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class UriOnlineJudgeWebScraper(email: String, password: String){
    companion object{
        private const val URL = "https://www.urionlinejudge.com.br/judge/pt"
        private const val URL_WEEKLY_RANK = "$URL/points/week"
        private const val TIMEOUT_MS = 500000
    }

    private var cookies: MutableMap<String, String> = mutableMapOf()

    init {
        login(email, password)
    }

    /**
     * Login Exception
     */
    class UriLoginException(override val message: String) : Exception(message)

    /**
     * Login to Uri Online Judge
     * @param email e-mail used for login
     * @param password user's password
     */
    private fun login(email:String, password: String){
        val connection = Jsoup.connect("$URL/login")

        cookies = connection
            .method(Connection.Method.GET)
            .execute()
            .cookies()

        val response = connection
            .method(Connection.Method.POST)
            .data("email", email)
            .data("password", password)
            .data("_csrfToken", cookies["csrfToken"])
            .cookies(cookies)
            .execute()

        if("'Desculpe, usuário ou senha inválido.'" in response.body())
            throw UriLoginException("Invalid user or password")

        response.cookies().forEach { cookies[it.key] = it.value }
    }

    /**
     * Find all users in weekly rank with a given name
     * @param name name of the user to find
     * @return a string containing the positions and points of the user if found or an empty string if not
     */
    fun getWeeklyRankPosition(name: String): List<String> {
        val (doc, result) = getWeeklyRankPositionByPage(1, name)
        val pages = doc.getElementById("table-info")?.text()?.replace(regex = "[0-9]* of ".toRegex(), replacement = "")?.toInt() ?: 1

        runBlocking {
            for(page in 2..pages){
                launch(Dispatchers.IO) {
                    val (_, result2) = getWeeklyRankPositionByPage(page, name)
                    result.addAll(result2)
                }
            }
        }
        return result.asSequence().sortedBy{it.first}.map{it.second}.toList()
    }

    /**
     * Find all users in weekly rank with a given name in the current given page
     * @param page weekly rank page index
     * @param name name of the user to find
     * @return a string containing the positions and points of the user if found or an empty string if not
     */
    private fun getWeeklyRankPositionByPage(page: Int, name:String): Pair<Document, MutableList<Pair<Int, String>>>{
        val response = Jsoup.connect("$URL_WEEKLY_RANK?page=$page")
            .method(Connection.Method.GET)
            .cookies(cookies)
            .timeout(TIMEOUT_MS)
            .execute()
        val doc = response.parse()
        return Pair(doc, doc.getElementsByTag("tbody")[0]
            .getElementsByTag("tr")
            .asSequence()
            .filter { it.text().contains(name, ignoreCase = true) }
            .map { weeklyRankPairFormatter(it.text()) }
            .toMutableList())
    }

    /**
     * Auxiliary function to format string output for {@link getWeeklyRankPosition}
     * @param input string in format like "Position Name University Points"
     * @return string formatted
     */
    private fun weeklyRankPairFormatter(input: String): Pair<Int, String>{
        val array = input.split(" ")
        return Pair(array.first().toInt(), buildString{
            this.appendLine("Position: ${array.first()}")
            this.appendLine("Name: ${buildString{
                this.append(array[1])
                for(i in 2..array.lastIndex-2)
                    this.append(" ${array[i]}")
            }}")
            this.appendLine("University: ${array[array.lastIndex-1]}")
            this.appendLine("Points: ${array.last()}")
        })
    }
}