package de.kuschku.quasseldroid_ng.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import de.kuschku.libquassel.protocol.BufferId
import de.kuschku.libquassel.protocol.Buffer_Type
import de.kuschku.libquassel.protocol.NetworkId
import de.kuschku.libquassel.quassel.BufferInfo
import de.kuschku.libquassel.quassel.syncables.BufferViewConfig
import de.kuschku.libquassel.quassel.syncables.IrcChannel
import de.kuschku.libquassel.quassel.syncables.IrcUser
import de.kuschku.libquassel.quassel.syncables.Network
import de.kuschku.libquassel.session.Backend
import de.kuschku.libquassel.session.ISession
import de.kuschku.libquassel.session.SessionManager
import de.kuschku.libquassel.util.and
import de.kuschku.libquassel.util.hasFlag
import de.kuschku.quasseldroid_ng.ui.chat.NickListAdapter
import de.kuschku.quasseldroid_ng.ui.chat.ToolbarFragment
import de.kuschku.quasseldroid_ng.ui.chat.buffers.BufferListAdapter
import de.kuschku.quasseldroid_ng.ui.chat.buffers.BufferViewConfigFragment
import de.kuschku.quasseldroid_ng.util.helper.map
import de.kuschku.quasseldroid_ng.util.helper.switchMap
import de.kuschku.quasseldroid_ng.util.helper.switchMapRx
import de.kuschku.quasseldroid_ng.util.helper.zip
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit

class QuasselViewModel : ViewModel() {
  private val backendWrapper = MutableLiveData<LiveData<Backend?>>()
  fun setBackend(backendWrapper: LiveData<Backend?>) {
    this.backendWrapper.value = backendWrapper
  }

  private val buffer = MutableLiveData<BufferId>()
  fun getBuffer(): LiveData<BufferId> = buffer
  fun setBuffer(buffer: BufferId) {
    this.buffer.value = buffer
  }

  private val bufferViewConfig = MutableLiveData<BufferViewConfig?>()
  fun getBufferViewConfig(): LiveData<BufferViewConfig?> = bufferViewConfig
  fun setBufferViewConfig(bufferViewConfig: BufferViewConfig?) {
    this.bufferViewConfig.value = bufferViewConfig
  }

  val backend = backendWrapper.switchMap { it }
  val sessionManager = backend.map { it.sessionManager() }
  val session = sessionManager.switchMapRx { it.session }

  val connectionProgress = sessionManager.switchMapRx { it.connectionProgress }

  private val bufferViewManager = session.map(ISession::bufferViewManager)

  private var lastMarkerLine = -1
  /**
   * An observable of the changes of the markerline, as pairs of `(old, new)`
   */
  val markerLine = session.switchMap { currentSession ->
    buffer.switchMapRx { currentBuffer ->
      // Get a stream of the latest marker line
      val raw = currentSession.bufferSyncer?.liveMarkerLine(currentBuffer)

      // Turn it into a pair of changes
      val changes = raw?.map {
        val previous = lastMarkerLine
        if (it != lastMarkerLine)
          lastMarkerLine = it
        previous to it
      }

      // Only return when there was an actual change
      val distinct = changes?.filter {
        it.first != it.second
      }

      distinct
    }
  }

  val lag: LiveData<Long?> = sessionManager.switchMapRx { it.session.switchMap { it.lag } }

  val isSecure: LiveData<Boolean?> = sessionManager.switchMapRx(SessionManager::session)
    .switchMapRx { session ->
      session.state.map { _ ->
        session.sslSession != null
      }
    }

  val bufferData = session.zip(buffer).switchMapRx { (session, id) ->
    val bufferSyncer = session?.bufferSyncer
    if (bufferSyncer != null) {
      bufferSyncer.liveBufferInfos().switchMap {
        val info = bufferSyncer.bufferInfo(id)
        val network = session.networks[info?.networkId]
        if (info == null || network == null) {
          Observable.just(ToolbarFragment.BufferData())
        } else {
          when (info.type.toInt()) {
            BufferInfo.Type.QueryBuffer.toInt()   -> {
              network.liveIrcUser(info.bufferName).switchMap { user ->
                user.live_realName.map { realName ->
                  ToolbarFragment.BufferData(
                    info = info,
                    network = network.networkInfo(),
                    description = realName
                  )
                }
              }
            }
            BufferInfo.Type.ChannelBuffer.toInt() -> {
              network.liveIrcChannel(
                info.bufferName
              ).switchMap { channel ->
                channel.live_topic.map { topic ->
                  ToolbarFragment.BufferData(
                    info = info,
                    network = network.networkInfo(),
                    description = topic
                  )
                }
              }
            }
            BufferInfo.Type.StatusBuffer.toInt()  -> {
              network.liveConnectionState.map {
                ToolbarFragment.BufferData(
                  info = info,
                  network = network.networkInfo()
                )
              }
            }
            else                                  -> Observable.just(
              ToolbarFragment.BufferData(
                description = "type is unknown: ${info.type.toInt()}"
              )
            )
          }
        }
      }
    } else {
      Observable.just(ToolbarFragment.BufferData())
    }
  }

  val nickData = session.zip(buffer).switchMapRx { (session, buffer) ->
    val bufferSyncer = session?.bufferSyncer
    val bufferInfo = bufferSyncer?.bufferInfo(buffer)
    if (bufferInfo?.type?.hasFlag(Buffer_Type.ChannelBuffer) == true) {
      val network = session.networks[bufferInfo.networkId]
      val ircChannel = network?.ircChannel(bufferInfo.bufferName)
      if (ircChannel != null) {
        Observable.combineLatest(
          ircChannel.ircUsers().map { user ->
            Observable.zip(
              user.live_realName, user.live_away,
              BiFunction<String, Boolean, Pair<String, Boolean>> { a, b -> Pair(a, b) }
            ).map { (realName, away) ->
              val userModes = ircChannel.userModes(user)
              val prefixModes = network.prefixModes()

              val lowestMode = userModes.mapNotNull {
                prefixModes.indexOf(it)
              }.min() ?: prefixModes.size

              NickListAdapter.IrcUserItem(
                user.nick(),
                network.modesToPrefixes(userModes),
                lowestMode,
                realName,
                away,
                network.support("CASEMAPPING")
              )
            }
          }, { array: Array<Any> ->
            array.toList() as List<NickListAdapter.IrcUserItem>
          }
        )
      } else {
        Observable.just(emptyList())
      }
    } else {
      Observable.just(emptyList())
    }
  }

  val bufferViewConfigs = bufferViewManager.switchMapRx { manager ->
    manager.liveBufferViewConfigs().map { ids ->
      ids.mapNotNull { id ->
        manager.bufferViewConfig(id)
      }.sortedWith(BufferViewConfig.NameComparator)
    }
  }

  val showHidden = MutableLiveData<Boolean>()
  val collapsedNetworks = MutableLiveData<Set<NetworkId>>()
  val selectedBufferId = MutableLiveData<BufferId>()
  val selectedBuffer = session.zip(
    selectedBufferId, bufferViewConfig
  ).switchMapRx { (session, buffer, bufferViewConfig) ->
    val bufferSyncer = session?.bufferSyncer
    if (bufferSyncer != null && bufferViewConfig != null) {
      val hiddenState = when {
        bufferViewConfig.removedBuffers().contains(buffer)            ->
          BufferListAdapter.HiddenState.HIDDEN_PERMANENT
        bufferViewConfig.temporarilyRemovedBuffers().contains(buffer) ->
          BufferListAdapter.HiddenState.HIDDEN_TEMPORARY
        else                                                          ->
          BufferListAdapter.HiddenState.VISIBLE
      }

      val info = bufferSyncer.bufferInfo(buffer)
      if (info != null) {
        val network = session.networks[info.networkId]
        when (info.type.enabledValues().firstOrNull()) {
          Buffer_Type.StatusBuffer  -> {
            network?.liveConnectionState?.map {
              BufferViewConfigFragment.SelectedItem(
                info,
                connectionState = it,
                hiddenState = hiddenState
              )
            }
          }
          Buffer_Type.ChannelBuffer -> {
            network?.liveIrcChannel(info.bufferName)?.map {
              BufferViewConfigFragment.SelectedItem(
                info,
                joined = it != IrcChannel.NULL,
                hiddenState = hiddenState
              )
            }
          }
          else                      ->
            Observable.just(BufferViewConfigFragment.SelectedItem(info, hiddenState = hiddenState))
        }
      } else {
        Observable.just(BufferViewConfigFragment.SelectedItem(info, hiddenState = hiddenState))
      }
    } else {
      Observable.just(BufferViewConfigFragment.SelectedItem())
    }
  }

  val bufferList: LiveData<Pair<BufferViewConfig?, List<BufferListAdapter.BufferProps>>?> = session.zip(
    bufferViewConfig, showHidden
  ).switchMapRx { (session, config, showHiddenRaw) ->
    val bufferSyncer = session?.bufferSyncer
    val showHidden = showHiddenRaw ?: false
    if (bufferSyncer != null && config != null) {
      config.live_config.debounce(16, TimeUnit.MILLISECONDS).switchMap { currentConfig ->
        Observable.combineLatest(
          listOf(
            config.live_buffers,
            config.live_temporarilyRemovedBuffers,
            config.live_removedBuffers
          ),
          object : Function<Array<Any>, List<Collection<BufferId>>> {
            override fun apply(objects: Array<Any>): List<Collection<BufferId>> {
              return objects.toList() as List<Collection<BufferId>>
            }
          }
        ).switchMap { (ids, temp, perm) ->
          fun transformIds(ids: Collection<BufferId>, state: BufferListAdapter.HiddenState) =
            ids.mapNotNull { id ->
              bufferSyncer.bufferInfo(id)
            }.filter {
                currentConfig.networkId() <= 0 || currentConfig.networkId() == it.networkId
              }.filter {
                (currentConfig.allowedBufferTypes() and it.type).isNotEmpty() ||
                it.type.hasFlag(Buffer_Type.StatusBuffer)
              }.mapNotNull {
                val network = session.networks[it.networkId]
                if (network == null) {
                  null
                } else {
                  it to network
                }
              }.map<Pair<BufferInfo, Network>, Observable<BufferListAdapter.BufferProps>?> { (info, network) ->
                bufferSyncer.liveActivity(info.bufferId).switchMap { activity ->
                  bufferSyncer.liveHighlightCount(info.bufferId).map { highlights ->
                    activity to highlights
                  }
                }.switchMap { (activity, highlights) ->
                    when (info.type.toInt()) {
                      BufferInfo.Type.QueryBuffer.toInt()   -> {
                        network.liveIrcUser(info.bufferName).switchMap { user ->
                          user.live_away.switchMap { away ->
                            user.live_realName.map { realName ->
                              BufferListAdapter.BufferProps(
                                info = info,
                                network = network.networkInfo(),
                                bufferStatus = when {
                                  user == IrcUser.NULL -> BufferListAdapter.BufferStatus.OFFLINE
                                  away                 -> BufferListAdapter.BufferStatus.AWAY
                                  else                 -> BufferListAdapter.BufferStatus.ONLINE
                                },
                                description = realName,
                                activity = activity,
                                highlights = highlights,
                                hiddenState = state
                              )
                            }
                          }
                        }
                      }
                      BufferInfo.Type.ChannelBuffer.toInt() -> {
                        network.liveIrcChannel(
                          info.bufferName
                        ).switchMap { channel ->
                          channel.live_topic.map { topic ->
                            BufferListAdapter.BufferProps(
                              info = info,
                              network = network.networkInfo(),
                              bufferStatus = when (channel) {
                                IrcChannel.NULL -> BufferListAdapter.BufferStatus.OFFLINE
                                else            -> BufferListAdapter.BufferStatus.ONLINE
                              },
                              description = topic,
                              activity = activity,
                              highlights = highlights,
                              hiddenState = state
                            )
                          }
                        }
                      }
                      BufferInfo.Type.StatusBuffer.toInt()  -> {
                        network.liveConnectionState.map {
                          BufferListAdapter.BufferProps(
                            info = info,
                            network = network.networkInfo(),
                            bufferStatus = BufferListAdapter.BufferStatus.OFFLINE,
                            description = "",
                            activity = activity,
                            highlights = highlights,
                            hiddenState = state
                          )
                        }
                      }
                      else                                  -> Observable.just(
                        BufferListAdapter.BufferProps(
                          info = info,
                          network = network.networkInfo(),
                          bufferStatus = BufferListAdapter.BufferStatus.OFFLINE,
                          description = "",
                          activity = activity,
                          highlights = highlights,
                          hiddenState = state
                        )
                      )
                    }
                  }
              }

          bufferSyncer.liveBufferInfos().switchMap {
            val buffers = if (showHidden) {
              transformIds(ids, BufferListAdapter.HiddenState.VISIBLE) +
              transformIds(temp, BufferListAdapter.HiddenState.HIDDEN_TEMPORARY) +
              transformIds(perm, BufferListAdapter.HiddenState.HIDDEN_PERMANENT)
            } else {
              transformIds(ids, BufferListAdapter.HiddenState.VISIBLE)
            }

            Observable.combineLatest(
              buffers,
              object : Function<Array<Any>, List<BufferListAdapter.BufferProps>> {
                override fun apply(objects: Array<Any>): List<BufferListAdapter.BufferProps> {
                  return objects.toList() as List<BufferListAdapter.BufferProps>
                }
              }
            ).map { list ->
              Pair<BufferViewConfig?, List<BufferListAdapter.BufferProps>>(
                config,
                list.filter {
                  (!config.hideInactiveBuffers()) ||
                  it.bufferStatus != BufferListAdapter.BufferStatus.OFFLINE ||
                  it.info.type.hasFlag(Buffer_Type.StatusBuffer)
                })
            }
          }
        }
      }
    } else {
      Observable.just(
        Pair<BufferViewConfig?, List<BufferListAdapter.BufferProps>>(null, emptyList())
      )
    }
  }

  init {
    showHidden.postValue(false)
    selectedBufferId.postValue(-1)
    collapsedNetworks.value = emptySet()
  }
}