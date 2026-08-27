package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Time.CriarTimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeResponse;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponse;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Seguranca.SegHelperService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeService {
    private final TimeRepository timeRepo;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepo;
    private final SegHelperService segHelperService;

    public void cadastrarTime(CriarTimeRequest dto, String nomeUsuarioCapitao){
        Time time = new Time();

        if(dto.getNomeTime()==null){
            throw new RegraDeNegocio("Nome do time NULL");
        }

        time.setNome(dto.getNomeTime());

        if(timeRepo.existsByNome(time.getNome())){
            throw new RegraDeNegocio("Nome de time ja utilizado");
        }

        List<String> nomesUsuarios = dto.getNomesUsuarios() != null ? dto.getNomesUsuarios() : new ArrayList<>();

        if(nomesUsuarios.size()>3){
            throw new RegraDeNegocio("Lista de membros do time maior que o permitido, tamanho= " + nomesUsuarios.size());
        }

        if(!nomesUsuarios.contains(nomeUsuarioCapitao)){
            throw new RegraDeNegocio("Lista de membros do time não contem o nome do capitão, usuario=" + nomeUsuarioCapitao);
        }

        if(!nomesUsuarios.isEmpty()) {
            for(String nome : nomesUsuarios){
                if(!usuarioRepo.existsByNomeUsuario(nome)){
                    throw new RegraDeNegocio("Usuario: " + nome + ", nao econtrado");
                }

                Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

                if(usuario.getTime() != null){
                    throw new RegraDeNegocio("Usuario: " + nome + ", ja esta cadastrado em um time");
                }

                time.getUsuarios().add(usuario);
                usuario.setTime(time);
            }
        }

        // A verificação se o usuario existe para ser capitão não é necessária pois é feita anteriormente
        time.setCapitao(usuarioRepo.findByNomeUsuario(nomeUsuarioCapitao));
        timeRepo.save(time);
    }

    // Paginado: a listagem crescia sem limite junto com a base.
    public Page<TimeResponse> listarTimes(Pageable paginacao){
        return timeRepo.findAll(paginacao).map(this::paraResponse);
    }

    private TimeResponse paraResponse(Time time){
        TimeResponse timeDTO = new TimeResponse();
        timeDTO.setNomeTime(time.getNome());

        // Mesmo cuidado do buscarTimeDoCapitao: getCapitao() pode ser null em time gravado
        // antes da coluna existir, e ali o acesso direto estourava NPE.
        timeDTO.setNomeCapitao(time.getCapitao() != null ? time.getCapitao().getNomeUsuario() : null);

        timeDTO.setUsuarios(new ArrayList<>());

        for(Usuario u : time.getUsuarios()){
            timeDTO.getUsuarios().add(UsuarioResponse.fromEntity(u));
        }

        return timeDTO;
    }

    public TimeResponse buscarTime(String nome){
        if(!timeRepo.existsByNome(nome)){
            throw new EntidadeNaoEcontrada("Time: "+ nome +", nao encontrado");
        }

        return paraResponse(timeRepo.findByNome(nome));
    }

    public void excluirTime(String nome, String nomeUsuarioCapitao){
        Time time = buscarTimeDoCapitao(nome, nomeUsuarioCapitao, "não pode excluir o time");

        for(Usuario u : time.getUsuarios()){
            u.setTime(null);
            usuarioRepo.save(u);
        }

        timeRepo.delete(time);
    }

    public void adicionarUsuarioNoTime(CriarTimeRequest dto, String nomeUsuarioCapitao){
        Time time = buscarTimeDoCapitao(dto.getNomeTime(), nomeUsuarioCapitao, "não pode adicionar integrante ao time");

        if(dto.getNomesUsuarios() == null || dto.getNomesUsuarios().isEmpty()){
            throw new RegraDeNegocio("Nenhum usuario para adicionar");
        }

        if((dto.getNomesUsuarios().size() + time.getUsuarios().size()) > 3){
            throw new RegraDeNegocio("Time: " + time.getNome() + ", tera mais de 3 integrantes");
        }

        for(String nome : dto.getNomesUsuarios()){
            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new EntidadeNaoEcontrada("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(usuario.getTime() != null){
                throw new RegraDeNegocio("Usuario: " + usuario.getNome() + ", ja esta no Time: " +
                        usuario.getTime().getNome());
            }

            usuario.setTime(time);
            time.getUsuarios().add(usuario);

            usuarioRepo.save(usuario);
            timeRepo.save(time);
        }
    }

    public void removerUsuarioNoTime(CriarTimeRequest dto, String nomeUsuarioCapitao){
        Time time = buscarTimeDoCapitao(dto.getNomeTime(), nomeUsuarioCapitao, "não pode remover integrante do time");

        if(dto.getNomesUsuarios() == null || dto.getNomesUsuarios().isEmpty()){
            throw new RegraDeNegocio("Nenhum usuario para remover");
        }

        for(String nome : dto.getNomesUsuarios()){
            // Antes da busca no banco de proposito: quem chama ja foi provado capitao pelo
            // buscarTimeDoCapitao, entao existe. O capitao sair sem passar a capitania
            // deixava capitao_id apontando para quem nao e mais integrante, e as escritas
            // seguiam passando no buscarTimeDoCapitao. Mesma logica do excluirConta, que
            // ja barra o capitao de apagar a propria conta.
            if(segHelperService.saoMesmoUsuario(nome, nomeUsuarioCapitao)){
                throw new RegraDeNegocio("O capitão não pode sair do time, Time= " + time.getNome() +
                        ". Transfira a capitania antes");
            }

            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new EntidadeNaoEcontrada("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(usuario.getTime() == null || !usuario.getTime().getNome().equals(time.getNome())){
                throw new RegraDeNegocio("Usuario: " + usuario.getNome() + ", nao esta no Time: " +
                        time.getNome());
            }

            time.getUsuarios().remove(usuario);
            timeRepo.save(time);
            usuario.setTime(null);
            usuarioRepo.save(usuario);
        }
    }

    public void editarNomeTime(String nomeTime, TimeRequest.AlterarNomeTime dto, String nomeUsuarioCapitao){
        Time time = buscarTimeDoCapitao(nomeTime, nomeUsuarioCapitao, "não pode renomear o time");

        String nomeNovo = dto.nomeTimeNovo();

        if(time.getNome().equals(nomeNovo)){
            throw new RegraDeNegocio("O novo nome do time deve ser diferente do atual");
        }

        if(timeRepo.existsByNome(nomeNovo)){
            throw new RegraDeNegocio("Nome de time ja utilizado");
        }

        time.setNome(nomeNovo);
        timeRepo.save(time);
    }

    public void transferirCapitania(String nomeTime, TimeRequest.TransferirCapitania dto, String nomeUsuarioCapitao){
        Time time = buscarTimeDoCapitao(nomeTime, nomeUsuarioCapitao, "não pode transferir a capitania");

        String nomeCapitaoNovo = dto.nomeCapitaoNovo();

        if(segHelperService.saoMesmoUsuario(nomeCapitaoNovo, nomeUsuarioCapitao)){
            throw new RegraDeNegocio("O novo capitão deve ser diferente do atual");
        }

        // Promover alguem de fora furaria o limite de 3 integrantes, entao o novo capitao
        // precisa ja estar no time -- mesma regra do cadastro, que exige o capitao na lista.
        Usuario capitaoNovo = null;
        for(Usuario u : time.getUsuarios()){
            if(segHelperService.saoMesmoUsuario(u.getNomeUsuario(), nomeCapitaoNovo)){
                capitaoNovo = u;
                break;
            }
        }

        if(capitaoNovo == null){
            throw new RegraDeNegocio("Usuario: " + nomeCapitaoNovo + ", nao e integrante do Time: " + time.getNome());
        }

        time.setCapitao(capitaoNovo);
        timeRepo.save(time);
    }

    // Toda escrita no time exige que quem chama seja o capitao. Centralizado porque a
    // checagem ja estava copiada em tres metodos, e as copias esqueciam que getCapitao()
    // pode ser null em times gravados antes da coluna existir (ddl-auto: update) -- ali o
    // acesso direto ao getNomeUsuario() estourava NPE, virando 500 em vez de 400.
    private Time buscarTimeDoCapitao(String nomeTime, String nomeUsuarioCapitao, String acao){
        if(nomeTime == null){
            throw new RegraDeNegocio("Parametro nome do time NULL");
        }

        if(!timeRepo.existsByNome(nomeTime)){
            throw new EntidadeNaoEcontrada("Time: " + nomeTime + ", nao encontrado");
        }

        Time time = timeRepo.findByNome(nomeTime);
        Usuario capitao = time.getCapitao();

        if(capitao == null || !segHelperService.saoMesmoUsuario(capitao.getNomeUsuario(), nomeUsuarioCapitao)){
            throw new RegraDeNegocio("Usuario não é o capitão do time, " + acao);
        }

        return time;
    }
}
