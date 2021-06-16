import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun main(){
    print("Type your e-mail: ")
    val email: String = readLine()!!

    print("Type your password: ")
    // Password in console will only be hidden when running the code outside the IDE
    val password: String = System.console()?.readPassword()?.toString() ?: readLine()!!

    print("Type the name to search: ")
    val name: String = readLine()!!

    try{
        val uri = UriOnlineJudgeWebScraper(email, password)
        val begin = System.currentTimeMillis()
        val result = uri.getWeeklyRankPosition(name)
        println()
        result.forEach { println(it) }
        val end = System.currentTimeMillis()
        val (minutes, seconds) = (end-begin).let{val div = it/1000; Pair(div/60, div%60)}
        println("time elapsed: $minutes minute(s) and $seconds second(s)")
    } catch (exception: ConnectException){
        println(exception)
    } catch (exception: SocketTimeoutException) {
        println("Internet connection problem! Please try again.")
    } catch (exception: UnknownHostException){
        println("Internet connection problem! Please try again.")
    } catch (exception: UriOnlineJudgeWebScraper.UriLoginException){
        println(exception.message)
    }
}