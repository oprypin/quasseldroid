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

package de.kuschku.quasseldroid.util.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v7.widget.PopupMenu
import android.widget.TextView
import de.kuschku.quasseldroid.R
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class LinkLongClickMenuHelper :
  BetterLinkMovementMethod.OnLinkLongClickListener,
  ((TextView, String) -> Boolean) {
  private var linkMenu: PopupMenu? = null

  override fun invoke(anchor: TextView, url: String) = onLongClick(anchor, url)
  override fun onLongClick(anchor: TextView, url: String?): Boolean {
    if (linkMenu == null) {
      linkMenu = PopupMenu(anchor.context, anchor).also { menu ->
        linkMenu?.dismiss()
        menu.menuInflater.inflate(R.menu.context_link, menu.menu)
        menu.setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.action_copy  -> {
              val clipboard = anchor.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText(null, url)
              clipboard.primaryClip = clip
              menu.dismiss()
              linkMenu = null
              true
            }
            R.id.action_share -> {
              val intent = Intent(Intent.ACTION_SEND)
              intent.type = "text/plain"
              intent.putExtra(Intent.EXTRA_TEXT, url)
              anchor.context.startActivity(
                Intent.createChooser(intent, anchor.context.getString(R.string.label_share))
              )
              menu.dismiss()
              linkMenu = null
              true
            }
            else              -> false
          }
        }
        menu.setOnDismissListener {
          linkMenu = null
        }
        menu.show()
      }
    }
    return true
  }
}
