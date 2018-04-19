package de.kuschku.quasseldroid.ui.clientsettings.whitelist

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.persistence.QuasselDatabase
import de.kuschku.quasseldroid.util.helper.visibleIf

class WhitelistCertificateAdapter :
  RecyclerView.Adapter<WhitelistCertificateAdapter.WhitelistItemViewHolder>() {
  private var clickListener: ((QuasselDatabase.SslValidityWhitelistEntry) -> Unit)? = null

  fun setOnClickListener(listener: ((QuasselDatabase.SslValidityWhitelistEntry) -> Unit)?) {
    clickListener = listener
  }

  private val data = mutableListOf<QuasselDatabase.SslValidityWhitelistEntry>()
  var list: List<QuasselDatabase.SslValidityWhitelistEntry>
    get() = data
    set(value) {
      val length = data.size
      data.clear()
      notifyItemRangeRemoved(0, length)
      data.addAll(value)
      notifyItemRangeInserted(0, list.size)
    }

  fun add(item: QuasselDatabase.SslValidityWhitelistEntry) {
    val index = data.size
    data.add(item)
    notifyItemInserted(index)
  }

  fun replace(index: Int, item: QuasselDatabase.SslValidityWhitelistEntry) {
    data[index] = item
    notifyItemChanged(index)
  }

  fun indexOf(item: QuasselDatabase.SslValidityWhitelistEntry) = data.indexOf(item)

  fun remove(index: Int) {
    data.removeAt(index)
    notifyItemRemoved(index)
  }

  fun remove(item: QuasselDatabase.SslValidityWhitelistEntry) = remove(indexOf(item))

  override fun getItemCount() = data.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WhitelistItemViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.preferences_whitelist_certificate_item,
                                                parent,
                                                false),
    ::remove
  )

  override fun onBindViewHolder(holder: WhitelistItemViewHolder, position: Int) {
    holder.bind(data[position])
  }

  class WhitelistItemViewHolder(
    itemView: View,
    clickListener: ((QuasselDatabase.SslValidityWhitelistEntry) -> Unit)?
  ) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.fingerprint)
    lateinit var fingerprint: TextView

    @BindView(R.id.ignore_date)
    lateinit var ignoreDate: View

    @BindView(R.id.action_delete)
    lateinit var delete: AppCompatImageButton

    private var item: QuasselDatabase.SslValidityWhitelistEntry? = null

    init {
      ButterKnife.bind(this, itemView)
      delete.setOnClickListener {
        item?.let {
          clickListener?.invoke(it)
        }
      }
    }

    fun bind(item: QuasselDatabase.SslValidityWhitelistEntry) {
      this.item = item
      fingerprint.text = item.fingerprint
      ignoreDate.visibleIf(item.ignoreDate)
    }
  }
}
