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

package de.kuschku.quasseldroid.ui.coresettings.chatlist

import de.kuschku.libquassel.util.helpers.value
import de.kuschku.quasseldroid.ui.coresettings.SettingsFragment

class ChatListEditFragment : ChatListBaseFragment(), SettingsFragment.Deletable {
  override fun onSave() = chatlist?.let { (it, data) ->
    applyChanges(data, it)
    it?.requestUpdate(data.toVariantMap())
    true
  } ?: false

  override fun onDelete() {
    chatlist?.let { (it, _) ->
      it?.let {
        viewModel.bufferViewManager.value?.orNull()?.requestDeleteBufferView(it.bufferViewId())
      }
    }
  }
}
