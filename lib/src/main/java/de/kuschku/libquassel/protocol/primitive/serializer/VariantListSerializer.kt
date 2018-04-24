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

package de.kuschku.libquassel.protocol.primitive.serializer

import de.kuschku.libquassel.protocol.QVariantList
import de.kuschku.libquassel.protocol.QVariant_
import de.kuschku.libquassel.quassel.QuasselFeatures
import de.kuschku.libquassel.util.nio.ChainedByteBuffer
import java.nio.ByteBuffer

object VariantListSerializer : Serializer<QVariantList> {
  override fun serialize(buffer: ChainedByteBuffer, data: QVariantList, features: QuasselFeatures) {
    IntSerializer.serialize(buffer, data.size, features)
    data.forEach {
      VariantSerializer.serialize(buffer, it, features)
    }
  }

  override fun deserialize(buffer: ByteBuffer, features: QuasselFeatures): QVariantList {
    val length = IntSerializer.deserialize(buffer, features)
    val result = mutableListOf<QVariant_>()
    for (i in 0 until length) {
      result.add(VariantSerializer.deserialize(buffer, features))
    }
    return result
  }
}
