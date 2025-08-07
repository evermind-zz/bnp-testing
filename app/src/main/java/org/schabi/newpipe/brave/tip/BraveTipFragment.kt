package org.schabi.newpipe.brave.tip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.layoutDirection
import androidx.fragment.app.Fragment
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.BraveItemTipComponentBinding
import org.schabi.newpipe.databinding.FragmentBraveTipBinding
import org.schabi.newpipe.util.Localization
import org.schabi.newpipe.util.external_communication.ShareUtils

/**
 * Fragment containing the tip possibilities.
 */
class BraveTipFragment : Fragment() {
    private lateinit var tipPossibilities: List<BraveTipComponent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipPossibilities = TIP_COMPONENTS
    }

    private fun View.openLink(url: String) {
        setOnClickListener {
            ShareUtils.openUrlInApp(context, url)
        }
    }

    private fun View.copyToClipboard(content: String) {
        setOnClickListener {
            ShareUtils.copyToClipboard(requireContext(), content)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBraveTipBinding.inflate(inflater, container, false)
        for (component in tipPossibilities) {
            val componentBinding = BraveItemTipComponentBinding.inflate(inflater, container, false)
            componentBinding.apply {
                name.text = component.name

                if (component.address != null) {
                    address.text = component.address
                    address.visibility = View.VISIBLE
                    componentBinding.root.copyToClipboard(component.address)
                }

                if (component.link != null) {
                    // link.openLink(component.link)
                    link.visibility = View.VISIBLE
                    componentBinding.root.openLink(component.link)

                    // the default icon is already set to R.drawable.brave_copy_icon
                    // here we replace it
                    val drawable =
                        ContextCompat.getDrawable(requireContext(), R.drawable.brave_link_icon)
                    val isLeftToRight =
                        Localization.getPreferredLocale(requireContext()).layoutDirection == View.LAYOUT_DIRECTION_LTR
                    if (isLeftToRight) {
                        name.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    } else {
                        name.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                    }
                }
            }

            val root: View = componentBinding.root
            binding.tipComponents.addView(root)
            registerForContextMenu(root)
        }
        return binding.root
    }

    companion object {
        val NAME: String = BraveTipFragment::class.java.simpleName
        private val TIP_COMPONENTS = arrayListOf(
            BraveTipComponent(
                "Ko-Fi", false, "https://ko-fi.com/BravePipe",
                null
            ),
            BraveTipComponent(
                "Bitcoin (Legacy address)", true, null,
                "1QGHXTzbgWAUn5hKz5x3vuTLpJPQjwPEFx"
            ),
            BraveTipComponent(
                "Bitcoin (Segwit address)", true, null,
                "3QLjHMX7tztZmzZEZXZ2Pgr4CX58ga21Y3"
            ),
            BraveTipComponent(
                "Bitcoin (Bech32 aka [Native Segwit])", true, null,
                "bc1qgwpvw3358z605ppxv2u99jlf558a2ksx0svvp5"
            ),
            BraveTipComponent(
                "Ethereum", true, null,
                "0x55bD8a63b556232fD164BcE3e1870C886db29Cc9"
            ),
            BraveTipComponent(
                "Dash", true, null,
                "Xn9PKVYwbLpwa2NMf9XRFCZXsbsZbGGjYU"
            ),
            BraveTipComponent(
                "Litecoin", true, null,
                "LX3WhHCh6Pn7kLLqLorqHWs4PoLPpdKmYB"
            ),
            BraveTipComponent(
                "Zcash", true, null,
                "t1gY1p9zdAFoxXamd333zq58nbSaKXXaAPj"
            ),
            BraveTipComponent(
                "Monero", true, null,
                "84rkqJ4jQxNNuxHHqXXgP84NnSpAAjKC9Udu7HbBLNyYZwauveqCLLGDbxjUR5SWCs7ftwXdLuV9UWvtmdsmWbiuScUMLjR"
            ),
        )
    }
}
