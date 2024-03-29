package br.com.silva.algafood.api.controller;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.silva.algafood.api.model.request.CreateRestauranteRequest;
import br.com.silva.algafood.api.model.request.UpdateRestauranteRequest;
import br.com.silva.algafood.domain.exception.EntidadeEmUsoException;
import br.com.silva.algafood.domain.exception.EntidadeNaoEncontradaException;
import br.com.silva.algafood.domain.model.Restaurante;
import br.com.silva.algafood.domain.service.CadastroRestauranteService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/restaurantes")
@AllArgsConstructor
public class RestauranteController {

	private CadastroRestauranteService cadastroRestaurante;

	@GetMapping
	public List<Restaurante> listar() {
		return cadastroRestaurante.listar();
	}

	@GetMapping("/{restauranteId}")
	public ResponseEntity<Restaurante> buscar(@PathVariable Long restauranteId) {
		Optional<Restaurante> restauranteOptional = cadastroRestaurante.buscar(restauranteId);

		return restauranteOptional.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public ResponseEntity<Object> adicionar(@RequestBody CreateRestauranteRequest request) {
		try {
			Restaurante restaurante = new Restaurante();
			BeanUtils.copyProperties(request, restaurante);
			restaurante = cadastroRestaurante.salvar(restaurante, request.getCozinhaId(), request.getFormasPagamento());
			return ResponseEntity.status(HttpStatus.CREATED).body(restaurante);
		} catch (EntidadeNaoEncontradaException e) {
			return ResponseEntity.badRequest()
					.body("Não existe cadastro de cozinha com código " + request.getCozinhaId());
		}
	}

	@DeleteMapping("/{restauranteId}")
	public ResponseEntity<Object> remover(@PathVariable Long restauranteId) {
		try {
			cadastroRestaurante.excluir(restauranteId);
			return ResponseEntity.noContent().build();
		} catch (EntidadeNaoEncontradaException e) {
			return ResponseEntity.notFound().build();
		} catch (EntidadeEmUsoException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@PutMapping("/{restauranteId}")
	public ResponseEntity<?> atualizar(@PathVariable Long restauranteId,
			@Valid @RequestBody UpdateRestauranteRequest request) {

		Optional<Restaurante> restauranteOptional = cadastroRestaurante.buscar(restauranteId);

		return restauranteOptional.map((restaurante) -> {
			restaurante.setNome(request.getNome());
			restaurante.setTaxaFrete(request.getTaxaFrete());
			return salvar(restaurante, request.getCozinhaId());
		}).orElse(ResponseEntity.notFound().build());
	}

	@PatchMapping("/{restauranteId}")
	public ResponseEntity<?> atualizarParcial(@PathVariable Long restauranteId,
			@RequestBody Map<String, Object> campos) {
		Optional<Restaurante> restauranteOptional = cadastroRestaurante.buscar(restauranteId);

		return restauranteOptional.map((restauranteAtual) -> {
			merge(campos, restauranteAtual);
			return salvar(restauranteAtual, restauranteAtual.getCozinha().getId());
		}).orElse(ResponseEntity.notFound().build());
	}

	private void merge(Map<String, Object> dadosOrigem, Restaurante restauranteDestino) {
		ObjectMapper objectMapper = new ObjectMapper();
		Restaurante restauranteOrigem = objectMapper.convertValue(dadosOrigem, Restaurante.class);

		dadosOrigem.forEach((nomePropriedade, valorPropriedade) -> {
			Field field = ReflectionUtils.findField(Restaurante.class, nomePropriedade);
			field.setAccessible(true);

			Object novoValor = ReflectionUtils.getField(field, restauranteOrigem);

			ReflectionUtils.setField(field, restauranteDestino, novoValor);
		});
	}

	private ResponseEntity<?> salvar(Restaurante restaurante, Long cozinhaId) {
		try {
			restaurante = cadastroRestaurante.salvar(restaurante, cozinhaId);
			return ResponseEntity.ok(restaurante);
		} catch (EntidadeNaoEncontradaException e) {
			return ResponseEntity.badRequest().body("Não existe cadastro de cozinha com código " + cozinhaId);
		}
	}

	private ResponseEntity<?> salvar(Restaurante restaurante, Long cozinhaId, List<Long> formasPagamentoId) {
		try {
			restaurante = cadastroRestaurante.salvar(restaurante, cozinhaId, formasPagamentoId);
			return ResponseEntity.ok(restaurante);
		} catch (EntidadeNaoEncontradaException e) {
			return ResponseEntity.badRequest().body("Não existe cadastro de cozinha com código " + cozinhaId);
		}
	}
}
