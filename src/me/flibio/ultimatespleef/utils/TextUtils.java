package me.flibio.ultimatespleef.utils;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import java.util.function.Consumer;

public class TextUtils {
	
	public static Text yesOption(Consumer<CommandSource> onClick) {
		Text yes = Text.builder("[").color(TextColors.DARK_GRAY).build();
		yes = yes.toBuilder().append(Text.builder("YES").color(TextColors.GREEN).build()).build();
		yes = yes.toBuilder().append(Text.builder("]").color(TextColors.DARK_GRAY).build()).build();
		
		yes = yes.toBuilder().onHover(TextActions.showText(Text.builder("YES!").color(TextColors.GREEN).build())).build();
		
		yes = yes.toBuilder().onClick(TextActions.executeCallback(onClick)).build();
		
		return yes;
	}
	
	public static Text noOption(Consumer<CommandSource> onClick) {
		Text no = Text.builder("[").color(TextColors.DARK_GRAY).build();
		no = no.toBuilder().append(Text.builder("NO").color(TextColors.RED).build()).build();
		no = no.toBuilder().append(Text.builder("]").color(TextColors.DARK_GRAY).build()).build();
		
		no = no.toBuilder().onHover(TextActions.showText(Text.builder("NO!").color(TextColors.RED).build())).build();
		
		no = no.toBuilder().onClick(TextActions.executeCallback(onClick)).build();
		
		return no;
	}
	
	public static Text option(Consumer<CommandSource> onClick, TextColor color, String option) {
		Text text = Text.builder("[").color(TextColors.DARK_GRAY).build();
		text = text.toBuilder().append(Text.builder(option).color(color).build()).build();
		text = text.toBuilder().append(Text.builder("]").color(TextColors.DARK_GRAY).build()).build();
		
		text = text.toBuilder().onHover(TextActions.showText(Text.builder(option).color(color).build())).build();
		
		text = text.toBuilder().onClick(TextActions.executeCallback(onClick)).build();
		
		return text;
	}
	
	public static Text message(String message) {
		Text text = Text.builder("Ultimate Spleef » ").color(TextColors.AQUA).build();
		return text.toBuilder().append(Text.of(TextColors.WHITE,message)).build();
	}
	
	public static Text message(Text message) {
		Text text = Text.builder("Ultimate Spleef » ").color(TextColors.AQUA).build();
		return text.toBuilder().append(Text.of(TextColors.WHITE,message)).build();
	}
	
}
