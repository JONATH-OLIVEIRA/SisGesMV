package br.com.sisgesmv.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.sisgesmv.enums.TipoUsuario;
import br.com.sisgesmv.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	
	Optional<Usuario> findByEmail(String email);

	Optional<Usuario> findByCpf(String cpf);

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

	List<Usuario> findByTipo(TipoUsuario admin);

}
