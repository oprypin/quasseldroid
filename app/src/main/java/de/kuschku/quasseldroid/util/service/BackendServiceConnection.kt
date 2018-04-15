package de.kuschku.quasseldroid.util.service

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

class BackendServiceConnection : ServiceConnection {
  val backend = BehaviorSubject.createDefault(Optional.empty<Backend>())

  var context: Context? = null

  override fun onServiceDisconnected(component: ComponentName?) {
    when (component) {
      ComponentName(context, QuasselService::class.java) -> {
        backend.onNext(Optional.empty())
      }
    }
  }

  override fun onServiceConnected(component: ComponentName?, binder: IBinder?) {
    when (component) {
      ComponentName(context, QuasselService::class.java) ->
        if (binder is QuasselBinder) {
          backend.onNext(Optional.of(binder.backend))
        }
    }
  }

  fun start(intent: Intent = QuasselService.intent(context!!)) {
    context?.startService(intent)
  }

  fun bind(intent: Intent = QuasselService.intent(context!!), flags: Int = 0) {
    context?.bindService(intent, this, flags)
  }

  fun stop(intent: Intent = QuasselService.intent(context!!)) {
    context?.stopService(intent)
  }

  fun unbind() {
    context?.unbindService(this)
  }
}
