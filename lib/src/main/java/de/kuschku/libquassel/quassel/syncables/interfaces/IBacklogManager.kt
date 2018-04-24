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

package de.kuschku.libquassel.quassel.syncables.interfaces

import de.kuschku.libquassel.annotations.Slot
import de.kuschku.libquassel.annotations.Syncable
import de.kuschku.libquassel.protocol.*
import de.kuschku.libquassel.protocol.Type

@Syncable(name = "BacklogManager")
interface IBacklogManager : ISyncableObject {
  @Slot
  fun requestBacklog(bufferId: BufferId, first: MsgId = -1, last: MsgId = -1, limit: Int = -1,
                     additional: Int = 0) {
    REQUEST(
      "requestBacklog", ARG(bufferId, QType.BufferId), ARG(first, QType.MsgId),
      ARG(last, QType.MsgId), ARG(limit, Type.Int), ARG(additional, Type.Int)
    )
  }

  @Slot
  fun requestBacklogAll(first: MsgId = -1, last: MsgId = -1, limit: Int = -1,
                        additional: Int = 0) {
    REQUEST(
      "requestBacklogAll", ARG(first, QType.MsgId), ARG(last, QType.MsgId),
      ARG(limit, Type.Int), ARG(additional, Type.Int)
    )
  }

  @Slot
  fun receiveBacklog(bufferId: BufferId, first: MsgId, last: MsgId, limit: Int, additional: Int,
                     messages: QVariantList)

  @Slot
  fun receiveBacklogAll(first: MsgId, last: MsgId, limit: Int, additional: Int,
                        messages: QVariantList)

  @Slot
  override fun update(properties: QVariantMap) {
    super.update(properties)
  }
}
