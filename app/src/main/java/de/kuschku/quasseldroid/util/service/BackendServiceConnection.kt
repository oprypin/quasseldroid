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

package de.kuschku.quasseldroid.util.service

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import de.kuschku.libquassel.session.Backend
import de.kuschku.libquassel.util.Optional
import de.kuschku.quasseldroid.service.QuasselBinder
import de.kuschku.quasseldroid.service.QuasselService
import io.reactivex.subjects.BehaviorSubject

class BackendServiceConnection : ServiceConnection, LifecycleObserver {
  val backend: BehaviorSubject<Optional<Backend>> = BehaviorSubject.createDefault(Optional.empty())

  var context: Context? = null

  enum class State {
    UNBOUND,
    BINDING,
    BOUND,
    UNBINDING
  }

  private var state: State = State.UNBOUND

  override fun onServiceDisconnected(component: ComponentName?) {
    synchronized(this@BackendServiceConnection) {
      state = State.UNBOUND
      backend.onNext(Optional.empty())
    }
  }

  override fun onServiceConnected(component: ComponentName?, binder: IBinder?) {
    when (component) {
      ComponentName(context, QuasselService::class.java) ->
        if (binder is QuasselBinder) {
          synchronized(this@BackendServiceConnection) {
            state = State.BOUND
            backend.onNext(Optional.of(binder.backend))
          }
        }
    }
  }

  fun start(intent: Intent = QuasselService.intent(context!!)) {
    context?.startService(intent)
  }

  @Synchronized
  fun bind(intent: Intent = QuasselService.intent(context!!), flags: Int = 0) {
    if (state == State.UNBOUND || state == State.UNBINDING) {
      state = State.BINDING
      context?.bindService(intent, this, flags)
    }
  }

  @Synchronized
  fun unbind() {
    if (state == State.BOUND || state == State.BINDING) {
      state = State.UNBINDING
      context?.unbindService(this)
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  fun onCreate(lifecycleOwner: LifecycleOwner) = start()

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart(lifecycleOwner: LifecycleOwner) = bind()

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop(lifecycleOwner: LifecycleOwner) = unbind()
}
