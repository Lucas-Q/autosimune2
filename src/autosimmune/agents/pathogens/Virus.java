package autosimmune.agents.pathogens;

import java.util.ArrayList;

import repast.simphony.annotate.AgentAnnot;
import repast.simphony.engine.schedule.ScheduledMethod;
import autosimmune.agents.Antigen;
import autosimmune.agents.cells.Cell;
import autosimmune.agents.cells.PC;
import autosimmune.defs.EnvParameters;
import autosimmune.env.Environment;
import autosimmune.env.Global;
import autosimmune.utils.Affinity;
import autosimmune.utils.Pattern;

/**
 * Classe que representa o Virus
 * @author maverick
 *
 */
@AgentAnnot(displayName = "Virus")
public class Virus extends Antigen {

	/** peptideo algo do virus */
	private Pattern target;
		
	/** referencia a celula infectada */
	private Cell host = null;
	
	/** numero de vezes que o DNA do virus foi executado pela celula hospedeira */
	private int hostTicks = 0;
	
	/**
	 * Construtor do Virus
	 * @param z Zona
	 * @param x Coordenada X
	 * @param y Coordenada Y
	 * @param target Cadeia de Peptideos Alvo
	 * @param antigen Cadeia de Peptideos do proprio virus
	 */
	public Virus(Environment z, int x, int y) {
		super(z, x, y, new Pattern(Global.getInstance().getStringParameter(EnvParameters.VIRUS_SELF_PATTERN)));
		this.target = new Pattern(Global.getInstance().getStringParameter(EnvParameters.VIRUS_TARGET_PATTERN));
	}
	
	/**
	 * Funcao que realiza, a cada tick, o comportamento do virus.
	 * Seu comportamento é simples: procura em sua volta por celulas
	 * que possa infectar, ou seja, que tenham afinidade com sua membrana.
	 * Caso achem, infectam e se reproduzem. Senão, andam aleatoriamente.
	 */
	@ScheduledMethod(start = 0, interval = 1)
	public void step(){
		
		tick();
		
		//procurando uma celula
		if (this.host == null ){
			randomWalk(true);
			//ArrayList<Cell> cells = super.getEspecificNeighbors(Cell.class);
			//for(Cell c: cells){
			ArrayList<PC> cells = super.getEspecificNeighbors(PC.class);
			for(PC c: cells){
				//o antigeno apresentado pelo MHC eh similar ao proprio antigeno da superficie da celula
				Pattern p = c.MHCI();
				if(Affinity.match(this.target, p)){
					if (this.infect(c)){
						return;
					}
				}
			}
		}
	}
	
	/** 
	 * Funcao chamada quando tenta infectar uma celula-alvo
	 * @param c Celula-alvo
	 */
	private boolean infect(Cell c) {
		if (c.infectedBy(this)){
			this.host = c;
			return true;
		}
		return false;
	}
	
	/**
	 * Remove o virus da celula. Funcao chamada
	 * quando a celula é destruida
	 * @param c
	 */
	public void removeHost(Cell c){
		if (this.host == c){
			this.host = null;
		}
	}

	/**
	 * Funcao do virus executada por uma celula hospedeira.
	 * É chamada pela propria celula hospedeira infectada.
	 * @param cell
	 */
	public void executeDNA(Cell cell) {
		if (host != null && host == cell){
			hostTicks++;
			int x = cell.getX();
			int y = cell.getY();
			this.moveTo(x, y);
			int virulency = Global.getInstance().getIntegerParameter(EnvParameters.VIRUS_VIRULENCY);
			int virusLatency = Global.getInstance().getIntegerParameter(EnvParameters.VIRUS_LATENCY);
			if (hostTicks % virusLatency == 0) {
				if (hostTicks > virusLatency){
					cell.necrosis();
					this.host = null;
					hostTicks = 0;
				} else {
					for(int i = 0; i < virulency; i++){
						Virus virus = new Virus(this.zone, this.getX(), this.getY());
						zone.addAgent(virus);
					}
				}
			}
		}
		
	}
	
	/**
	 * Funcao chamada quando o Virus é neutralizado. Quando o Macrophage fagocita-o, por exemplo
	 * @return true se foi possivel neutraliza-lo (Virus intracelulares retornam false, por exemplo)
	 */
	public boolean neutralize(){
		return neutralize(false);
	}
	
	public boolean neutralize(boolean force){
		if(this.host == null){
			this.die();
			return true;
		} else if (force) {
			this.removeHost(this.host);
			this.host = null;
			this.die();
			return true;
		} else {
			return false;
		}
	}

}
