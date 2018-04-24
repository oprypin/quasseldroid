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

package de.kuschku.libquassel.quassel

import de.kuschku.libquassel.util.flag.Flag
import de.kuschku.libquassel.util.flag.Flags

enum class ProtocolFeature(override val bit: Int) :
  Flag<ProtocolFeature> {
  None(0x00),
  TLS(0x01),
  Compression(0x02);

  companion object : Flags.Factory<ProtocolFeature> {
    override val NONE = ProtocolFeature.of()
    override fun of(bit: Int) = Flags.of<ProtocolFeature>(bit)
    override fun of(vararg flags: ProtocolFeature) = Flags.of(*flags)
    override fun of(flags: Iterable<ProtocolFeature>) = Flags.of(flags)
  }
}
