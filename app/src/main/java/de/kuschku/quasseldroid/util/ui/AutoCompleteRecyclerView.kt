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

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import de.kuschku.quasseldroid.R

class AutoCompleteRecyclerView : RecyclerView {
  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
    context, attrs, defStyle
  )

  override fun onMeasure(widthSpec: Int, heightSpec: Int) {
    super.onMeasure(
      widthSpec,
      MeasureSpec.makeMeasureSpec(
        resources.getDimensionPixelSize(R.dimen.autocomplete_max_height),
        MeasureSpec.AT_MOST
      )
    )
  }
}
