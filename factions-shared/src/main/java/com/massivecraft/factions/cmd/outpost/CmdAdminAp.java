package com.massivecraft.factions.cmd.outpost;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;

import java.util.Collections;

public class CmdAdminAp extends FCommand {

    public final CmdAdminApWand cmdAdminApWand = new CmdAdminApWand();
    public final CmdAdminApSave cmdAdminApSave = new CmdAdminApSave();
    public final CmdAdminApSet cmdAdminApSet = new CmdAdminApSet();
    public final CmdAdminApUnset cmdAdminApUnset = new CmdAdminApUnset();

    public CmdAdminAp() {
        super();
        this.getAliases().addAll(Collections.singletonList("ap"));
        this.addSubCommand(this.cmdAdminApWand);
        this.addSubCommand(this.cmdAdminApSave);
        this.addSubCommand(this.cmdAdminApSet);
        this.addSubCommand(this.cmdAdminApUnset);

        this.setRequirements(new CommandRequirements.Builder(Permission.OUTPOST_ADMIN).build());
    }

    @Override
    public void perform(CommandContext context) {
        context.commandChain.add(this);
        FactionsPlugin.getInstance().cmdAutoHelp.execute(context);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}