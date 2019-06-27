package org.openhab.binding.melcloud.internal;

import static org.openhab.binding.melcloud.internal.MelCloudBindingConstants.CHANNEL_SET_TEMPERATURE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { DynamicStateDescriptionProvider.class,
        MelcloudDynamicStateDescriptionProvider.class }, immediate = true)
@NonNullByDefault
public class MelcloudDynamicStateDescriptionProvider implements DynamicStateDescriptionProvider {

    private final Map<ChannelUID, @Nullable List<StateOption>> channelOptionsMap = new ConcurrentHashMap<>();
    private final Map<ChannelUID, @Nullable StateAttribute> channelStateAttributesMap = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(MelcloudDynamicStateDescriptionProvider.class);

    /**
     * For a given channel UID, set a {@link List} of {@link StateOption}s that should be used for the channel, instead
     * of the one defined statically in the {@link ChannelType}.
     *
     * @param channelUID the channel UID of the channel
     * @param options    a {@link List} of {@link StateOption}s
     */
    public void setStateOptions(ChannelUID channelUID, @Nullable List<StateOption> options,
            @Nullable StateAttribute stateAttribute) {
        logger.debug("setStateOptions : " + channelUID.getId());
        channelOptionsMap.put(channelUID, options);
        channelStateAttributesMap.put(channelUID, stateAttribute);

    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        List<StateOption> options = channelOptionsMap.get(channel.getUID());
        logger.debug("MelcloudDynamicStateDescriptionProvider : getStateDescription : " + channel.getUID().getId());
        StateAttribute stateAttribute = channelStateAttributesMap.get(channel.getUID());
        if (options == null || stateAttribute == null) {
            if (options == null) {
                logger.debug("option null");
            }
            if (stateAttribute == null) {
                logger.debug("stateAttribute null");
            }
            return null;
        }

        StateDescriptionFragmentBuilder builder = (originalStateDescription == null)
                ? StateDescriptionFragmentBuilder.create()
                : StateDescriptionFragmentBuilder.create(originalStateDescription);

        if (channel.getUID().getId().equals(CHANNEL_SET_TEMPERATURE)) {
            logger.debug("Min temp :" + stateAttribute.getMin());
            return builder.withMinimum(stateAttribute.getMin()).withMaximum(stateAttribute.getMax()).build()
                    .toStateDescription();
        }

        return builder.withOptions(options).build().toStateDescription();
    }

    @Deactivate
    public void deactivate() {
        channelOptionsMap.clear();
    }

}
