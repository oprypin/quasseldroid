package de.kuschku.quasseldroid.ui.chat.buffers

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.AdapterView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import de.kuschku.libquassel.protocol.BufferId
import de.kuschku.libquassel.protocol.Buffer_Activity
import de.kuschku.libquassel.protocol.Buffer_Type
import de.kuschku.libquassel.protocol.Message_Type
import de.kuschku.libquassel.quassel.syncables.interfaces.INetwork
import de.kuschku.libquassel.util.hasFlag
import de.kuschku.libquassel.util.minus
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.persistence.QuasselDatabase
import de.kuschku.quasseldroid.settings.AppearanceSettings
import de.kuschku.quasseldroid.util.helper.map
import de.kuschku.quasseldroid.util.helper.zip
import de.kuschku.quasseldroid.util.irc.format.IrcFormatDeserializer
import de.kuschku.quasseldroid.util.service.ServiceBoundFragment
import de.kuschku.quasseldroid.viewmodel.QuasselViewModel
import de.kuschku.quasseldroid.viewmodel.data.BufferHiddenState
import javax.inject.Inject

class BufferViewConfigFragment : ServiceBoundFragment() {
  @BindView(R.id.chatListToolbar)
  lateinit var chatListToolbar: Toolbar

  @BindView(R.id.chatListSpinner)
  lateinit var chatListSpinner: AppCompatSpinner

  @BindView(R.id.chatList)
  lateinit var chatList: RecyclerView

  @Inject
  lateinit var appearanceSettings: AppearanceSettings

  @Inject
  lateinit var database: QuasselDatabase

  @Inject
  lateinit var ircFormatDeserializer: IrcFormatDeserializer

  private lateinit var viewModel: QuasselViewModel

  private var actionMode: ActionMode? = null

  private val actionModeCallback = object : ActionMode.Callback {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
      val selected = viewModel.selectedBuffer.value
      val info = selected?.info
      val session = viewModel.session.value
      val bufferSyncer = session?.bufferSyncer
      val network = session?.networks?.get(selected?.info?.networkId)
      val bufferViewConfig = viewModel.bufferViewConfig.value

      return if (info != null && session != null) {
        when (item?.itemId) {
          R.id.action_connect    -> {
            network?.requestConnect()
            actionMode?.finish()
            true
          }
          R.id.action_disconnect -> {
            network?.requestDisconnect()
            actionMode?.finish()
            true
          }
          R.id.action_join       -> {
            session.rpcHandler?.sendInput(info, "/join ${info.bufferName}")
            actionMode?.finish()
            true
          }
          R.id.action_part       -> {
            session.rpcHandler?.sendInput(info, "/part ${info.bufferName}")
            actionMode?.finish()
            true
          }
          R.id.action_delete     -> {
            MaterialDialog.Builder(activity!!)
              .content(R.string.buffer_delete_confirmation)
              .positiveText(R.string.label_yes)
              .negativeText(R.string.label_no)
              .negativeColorAttr(R.attr.colorTextPrimary)
              .backgroundColorAttr(R.attr.colorBackgroundCard)
              .contentColorAttr(R.attr.colorTextPrimary)
              .onPositive { _, _ ->
                selected.info?.let {
                  session.bufferSyncer?.requestRemoveBuffer(info.bufferId)
                }
              }
              .build()
              .show()
            actionMode?.finish()
            true
          }
          R.id.action_rename     -> {
            MaterialDialog.Builder(activity!!)
              .input(
                getString(R.string.label_buffer_name),
                info.bufferName,
                false
              ) { _, input ->
                selected.info?.let {
                  session.bufferSyncer?.requestRenameBuffer(info.bufferId, input.toString())
                }
              }
              .positiveText(R.string.label_save)
              .negativeText(R.string.label_cancel)
              .negativeColorAttr(R.attr.colorTextPrimary)
              .backgroundColorAttr(R.attr.colorBackgroundCard)
              .contentColorAttr(R.attr.colorTextPrimary)
              .build()
              .show()
            actionMode?.finish()
            true
          }
          R.id.action_unhide     -> {
            bufferSyncer?.let {
              bufferViewConfig?.requestAddBuffer(info, bufferSyncer)
            }
            true
          }
          R.id.action_hide_temp  -> {
            bufferViewConfig?.requestRemoveBuffer(info.bufferId)
            true
          }
          R.id.action_hide_perm  -> {
            bufferViewConfig?.requestRemoveBufferPermanently(info.bufferId)
            true
          }
          else                   -> false
        }
      } else {
        false
      }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
      actionMode = mode
      mode?.menuInflater?.inflate(R.menu.context_buffer, menu)
      return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
      return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
      actionMode = null
      listAdapter.unselectAll()
    }
  }

  private lateinit var listAdapter: BufferListAdapter

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    viewModel = ViewModelProviders.of(activity!!)[QuasselViewModel::class.java]
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
    ButterKnife.bind(this, view)

    val adapter = BufferViewConfigAdapter(this, viewModel.bufferViewConfigs)

    chatListSpinner.adapter = adapter
    chatListSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(p0: AdapterView<*>?) {
        viewModel.setBufferViewConfigId(null)
      }

      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        viewModel.setBufferViewConfigId(adapter.getItem(p2)?.bufferViewId())
      }
    }

    listAdapter = BufferListAdapter(
      this,
      viewModel.bufferList.zip(database.filtered().listen(accountId)).map {
        val (data, activityList) = it
        val (config, list) = data ?: Pair(null, emptyList())
        val minimumActivity = config?.minimumActivity() ?: Buffer_Activity.NONE
        val activities = activityList.map { it.bufferId to it.filtered }.toMap()
        list.map {
          val activity = it.activity - (activities[it.info.bufferId] ?: 0)
          it.copy(
            description = ircFormatDeserializer.formatString(
              requireContext(), it.description.toString(), appearanceSettings.colorizeMirc
            ),
            activity = activity,
            bufferActivity = Buffer_Activity.of(
              when {
                it.highlights > 0                     -> Buffer_Activity.Highlight
                activity.hasFlag(Message_Type.Plain) ||
                activity.hasFlag(Message_Type.Notice) ||
                activity.hasFlag(Message_Type.Action) -> Buffer_Activity.NewMessage
                activity.isNotEmpty()                 -> Buffer_Activity.OtherActivity
                else                                  -> Buffer_Activity.NoActivity
              }
            )
          )
        }.filter { props ->
          minimumActivity.toInt() <= props.bufferActivity.toInt() ||
          props.info.type.hasFlag(Buffer_Type.StatusBuffer)
        }
      },
      viewModel.selectedBufferId,
      viewModel.collapsedNetworks,
      ::runInBackground,
      activity!!::runOnUiThread,
      clickListener,
      longClickListener
    )
    chatList.adapter = listAdapter

    viewModel.selectedBuffer.observe(this, Observer { buffer ->
      if (buffer != null) {
        val menu = actionMode?.menu
        if (menu != null) {
          val allActions = setOf(
            R.id.action_connect,
            R.id.action_disconnect,
            R.id.action_join,
            R.id.action_part,
            R.id.action_delete,
            R.id.action_rename,
            R.id.action_unhide,
            R.id.action_hide_temp,
            R.id.action_hide_perm
          )

          val visibilityActions = when (buffer.hiddenState) {
            BufferHiddenState.VISIBLE          -> setOf(
              R.id.action_hide_temp,
              R.id.action_hide_perm
            )
            BufferHiddenState.HIDDEN_TEMPORARY -> setOf(
              R.id.action_unhide,
              R.id.action_hide_perm
            )
            BufferHiddenState.HIDDEN_PERMANENT -> setOf(
              R.id.action_unhide,
              R.id.action_hide_temp
            )
          }

          val availableActions = when (buffer.info?.type?.enabledValues()?.firstOrNull()) {
            Buffer_Type.StatusBuffer  -> {
              when (buffer.connectionState) {
                INetwork.ConnectionState.Disconnected -> setOf(R.id.action_connect)
                INetwork.ConnectionState.Initialized  -> setOf(R.id.action_disconnect)
                else                                  -> setOf(
                  R.id.action_connect, R.id.action_disconnect
                )
              }
            }
            Buffer_Type.ChannelBuffer -> {
              if (buffer.joined) {
                setOf(R.id.action_part)
              } else {
                setOf(R.id.action_join, R.id.action_delete)
              } + visibilityActions
            }
            Buffer_Type.QueryBuffer   -> {
              setOf(R.id.action_delete, R.id.action_rename) + visibilityActions
            }
            else                      -> visibilityActions
          }

          val unavailableActions = allActions - availableActions

          for (action in availableActions) {
            menu.findItem(action)?.isVisible = true
          }
          for (action in unavailableActions) {
            menu.findItem(action)?.isVisible = false
          }
        }
      } else {
        actionMode?.finish()
      }
    })

    chatListToolbar.inflateMenu(R.menu.context_bufferlist)
    chatListToolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.action_show_hidden -> {
          item.isChecked = !item.isChecked
          viewModel.showHidden.value = item.isChecked
          true
        }
        else                    -> false
      }
    }
    chatList.layoutManager = LinearLayoutManager(context)
    //chatList.itemAnimator = DefaultItemAnimator()
    chatList.setItemViewCacheSize(10)
    return view
  }

  private val clickListener: ((BufferId) -> Unit)? = {
    if (actionMode != null) {
      longClickListener?.invoke(it)
    } else {
      viewModel.buffer.value = it
    }
  }

  private val longClickListener: ((BufferId) -> Unit)? = {
    if (actionMode == null) {
      chatListToolbar.startActionMode(actionModeCallback)
    }
    listAdapter.toggleSelection(it)
  }
}