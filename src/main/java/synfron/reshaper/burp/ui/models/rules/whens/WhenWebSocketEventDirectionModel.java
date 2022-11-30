package synfron.reshaper.burp.ui.models.rules.whens;

import lombok.Getter;
import synfron.reshaper.burp.core.ProtocolType;
import synfron.reshaper.burp.core.messages.WebSocketDataDirection;
import synfron.reshaper.burp.core.rules.whens.WhenWebSocketEventDirection;
import synfron.reshaper.burp.ui.models.rules.RuleOperationModelType;

public class WhenWebSocketEventDirectionModel extends WhenModel<WhenWebSocketEventDirectionModel, WhenWebSocketEventDirection> {

    @Getter
    private WebSocketDataDirection dataDirection;

    public WhenWebSocketEventDirectionModel(ProtocolType protocolType, WhenWebSocketEventDirection when, Boolean isNew) {
        super(protocolType, when, isNew);
        dataDirection = when.getDataDirection();
    }

    public void setDataDirection(WebSocketDataDirection dataDirection) {
        this.dataDirection = dataDirection;
        propertyChanged("dataDirection", dataDirection);
    }

    public boolean persist() {
        if (validate().size() != 0) {
            return false;
        }
        ruleOperation.setDataDirection(dataDirection);
        return super.persist();
    }

    @Override
    public boolean record() {
        if (validate().size() != 0) {
            return false;
        }
        setValidated(true);
        return true;
    }

    @Override
    public RuleOperationModelType<WhenWebSocketEventDirectionModel, WhenWebSocketEventDirection> getType() {
        return WhenModelType.WebSocketEventDirection;
    }
}
