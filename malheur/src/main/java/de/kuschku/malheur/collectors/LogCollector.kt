/*
 * QuasselDroid - Quassel client for Android
 *
 * Copyright (c) 2018 Janne Koschinski
 * Copyright (c) 2018 Ken Børge Viktil
 * Copyright (c) 2018 Magnus Fjell
 * Copyright (c) 2018 Martin Sandsmark
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kuschku.malheur.collectors

import de.kuschku.malheur.CrashContext
import de.kuschku.malheur.config.LogConfig
import java.text.SimpleDateFormat
import java.util.*

class LogCollector : Collector<Map<String, List<String>>, LogConfig> {
  private val logcatTimeFormatter = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

  override fun collect(context: CrashContext, config: LogConfig): Map<String, List<String>> {
    val since = logcatTimeFormatter.format(context.startTime)
    return config.buffers.map { buffer ->
      buffer to readLogCat(since, buffer)
    }.toMap()
  }

  private fun readLogCat(since: String, buffer: String) = ProcessBuilder()
    .command("logcat", "-t", since, "-b", buffer)
    .redirectErrorStream(true)
    .start()
    .inputStream
    .bufferedReader(Charsets.UTF_8)
    .readLines()
}
