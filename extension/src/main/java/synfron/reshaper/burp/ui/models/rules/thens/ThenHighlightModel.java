package synfron.reshaper.burp.ui.models.rules.thens;

import lombok.Getter;
import synfron.reshaper.burp.core.ProtocolType;
import synfron.reshaper.burp.core.rules.thens.ThenHighlight;
import synfron.reshaper.burp.ui.models.rules.RuleOperationModelType;

public class ThenHighlightModel extends ThenModel<ThenHighlightModel, ThenHighlight> {

    @Getter
    private ThenHighlight.HighlightColor color;

    public ThenHighlightModel(ProtocolType protocolType, ThenHighlight then, Boolean isNew) {
        super(protocolType, then, isNew);
        color = then.getColor();
    }

    public void setColor(ThenHighlight.HighlightColor color) {
        this.color = color;
        propertyChanged("color", color);
    }

    public boolean persist() {
        if (validate().size() != 0) {
            return false;
        }
        ruleOperation.setColor(color);
        setValidated(true);
        return true;
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
    protected String getTargetName() {
        return color.name();
    }

    @Override
    public RuleOperationModelType<ThenHighlightModel, ThenHighlight> getType() {
        return ThenModelType.Highlight;
    }
}
