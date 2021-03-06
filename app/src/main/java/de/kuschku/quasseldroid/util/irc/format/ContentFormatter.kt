/*
 * Quasseldroid - Quassel client for Android
 *
 * Copyright (c) 2018 Janne Koschinski
 * Copyright (c) 2018 The Quassel Project
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kuschku.quasseldroid.util.irc.format

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.ColorInt
import de.kuschku.libquassel.protocol.NetworkId
import de.kuschku.libquassel.util.irc.HostmaskHelper
import de.kuschku.libquassel.util.irc.SenderColorUtil
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.settings.MessageSettings
import de.kuschku.quasseldroid.ui.chat.ChatActivity
import de.kuschku.quasseldroid.util.helper.styledAttributes
import de.kuschku.quasseldroid.util.ui.SpanFormatter
import org.intellij.lang.annotations.Language
import javax.inject.Inject

class ContentFormatter @Inject constructor(
  context: Context,
  private val ircFormatDeserializer: IrcFormatDeserializer,
  private val messageSettings: MessageSettings
) {
  @Language("RegExp")
  private val scheme = "(?:(?:mailto:|magnet:|(?:[+.-]?\\w)+://)|www(?=\\.\\S+\\.))"
  @Language("RegExp")
  private val authority = "(?:(?:[,.;@:]?[-\\w]+)+\\.?|\\[[0-9a-f:.]+])?(?::\\d+)?"
  @Language("RegExp")
  private val urlChars = "(?:[,.;:]*[\\w~@/?&=+$()!%#*-])"
  @Language("RegExp")
  private val urlEnd = "((?:>|[,.;:\"]*\\s|\\b|$))"

  private val urlPattern = Regex(
    "\\b($scheme$authority(?:$urlChars*)?)$urlEnd",
    RegexOption.IGNORE_CASE
  )

  private val channelPattern = Regex(
    "((?:#|![A-Z0-9]{5})[^,:\\s]+(?::[^,:\\s]+)?)\\b",
    RegexOption.IGNORE_CASE
  )

  private val senderColors: IntArray = context.theme.styledAttributes(
    R.attr.senderColor0, R.attr.senderColor1, R.attr.senderColor2, R.attr.senderColor3,
    R.attr.senderColor4, R.attr.senderColor5, R.attr.senderColor6, R.attr.senderColor7,
    R.attr.senderColor8, R.attr.senderColor9, R.attr.senderColorA, R.attr.senderColorB,
    R.attr.senderColorC, R.attr.senderColorD, R.attr.senderColorE, R.attr.senderColorF
  ) {
    IntArray(16) {
      getColor(it, 0)
    }
  }

  @ColorInt
  private val selfColor: Int = context.theme.styledAttributes(R.attr.colorForegroundSecondary) {
    getColor(0, 0)
  }

  class QuasselURLSpan(text: String, private val highlight: Boolean) : URLSpan(text) {
    override fun updateDrawState(ds: TextPaint?) {
      if (ds != null) {
        if (!highlight) ds.color = ds.linkColor
        ds.isUnderlineText = true
      }
    }
  }

  class ChannelLinkSpan(private val networkId: NetworkId, private val text: String,
                        private val highlight: Boolean) : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint?) {
      if (ds != null) {
        if (!highlight) ds.color = ds.linkColor
        ds.isUnderlineText = true
      }
    }

    override fun onClick(widget: View) {
      ChatActivity.launch(
        widget.context,
        networkId = networkId,
        channel = text
      )
    }
  }

  fun formatContent(content: String,
                    highlight: Boolean = false,
                    networkId: NetworkId?): CharSequence {
    val formattedText = ircFormatDeserializer.formatString(content, messageSettings.colorizeMirc)
    val text = SpannableString(formattedText)

    for (result in urlPattern.findAll(formattedText)) {
      val group = result.groups[1]
      if (group != null) {
        text.setSpan(
          QuasselURLSpan(group.value, highlight), group.range.start,
          group.range.start + group.value.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
      }
    }

    if (networkId != null) {
      for (result in channelPattern.findAll(formattedText)) {
        val group = result.groups[1]
        if (group != null) {
          text.setSpan(ChannelLinkSpan(networkId, group.value, highlight),
                       group.range.start,
                       group.range.endInclusive + 1,
                       Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
      }
    }

    return text
  }

  private fun formatNickNickImpl(nick: String, self: Boolean, colorize: Boolean,
                                 senderColors: IntArray, @ColorInt selfColor: Int): CharSequence {
    val spannableString = SpannableString(nick)
    if (colorize) {
      val color = if (self) selfColor
      else senderColors[(SenderColorUtil.senderColor(nick) + senderColors.size) % senderColors.size]
      spannableString.setSpan(
        ForegroundColorSpan(color),
        0,
        nick.length,
        SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
      )
    }
    spannableString.setSpan(
      StyleSpan(Typeface.BOLD),
      0,
      nick.length,
      SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
    )
    return spannableString
  }

  private fun formatNickImpl(sender: String, self: Boolean, colorize: Boolean, hostmask: Boolean,
                             senderColors: IntArray, @ColorInt selfColor: Int): CharSequence {
    val (nick, user, host) = HostmaskHelper.split(sender)
    val formattedNick = formatNickNickImpl(nick, self, colorize, senderColors, selfColor)

    return if (hostmask) {
      SpanFormatter.format("%s (%s@%s)", formattedNick, user, host)
    } else {
      formattedNick
    }
  }

  fun formatNick(sender: String, self: Boolean = false, highlight: Boolean = false,
                 showHostmask: Boolean = false, senderColors: IntArray = this.senderColors,
                 @ColorInt selfColor: Int = this.selfColor) =
    when (messageSettings.colorizeNicknames) {
      MessageSettings.ColorizeNicknamesMode.ALL          ->
        formatNickImpl(sender, false, !highlight, showHostmask, senderColors, selfColor)
      MessageSettings.ColorizeNicknamesMode.ALL_BUT_MINE ->
        formatNickImpl(sender, self, !highlight, showHostmask, senderColors, selfColor)
      MessageSettings.ColorizeNicknamesMode.NONE         ->
        formatNickImpl(sender, false, false, showHostmask, senderColors, selfColor)
    }

  fun formatPrefix(prefix: String) = when (messageSettings.showPrefix) {
    MessageSettings.ShowPrefixMode.ALL     -> prefix
    MessageSettings.ShowPrefixMode.HIGHEST -> prefix.substring(0, Math.min(prefix.length, 1))
    MessageSettings.ShowPrefixMode.NONE    -> ""
  }
}
