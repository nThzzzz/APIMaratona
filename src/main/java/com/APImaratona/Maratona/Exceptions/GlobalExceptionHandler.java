package com.APImaratona.Maratona.Exceptions;

import com.APImaratona.Maratona.DTO.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Captura nossos erros de Regra de Negócio e transforma em 400 (BAD REQUEST)
    @ExceptionHandler(RegraDeNegocio.class)
    public ResponseEntity<ErrorResponseDTO> handleRegraNegocioException(RegraDeNegocio ex) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // Captura quando não achamos algo e transforma em 404 (NOT FOUND)
    @ExceptionHandler(EntidadeNaoEcontrada.class)
    public ResponseEntity<ErrorResponseDTO> handleEntidadeNaoEncontradaException(EntidadeNaoEcontrada ex) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    // Captura os erros das anotações @NotBlank, @Email, etc.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacaoCampos(MethodArgumentNotValidException ex) {

        // Pega a mensagem do primeiro campo que falhou (ex: "O nome é obrigatório")
        String mensagemValidacao = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação de Campos",
                mensagemValidacao
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // Captura falha de login ou token usado contra a conta errada, retorna 401 (UNAUTHORIZED)
    @ExceptionHandler(AutenticacaoInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> handleAutenticacaoInvalida(AutenticacaoInvalidaException ex) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Falha de Autenticação",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    // Captura qualquer outro erro que não prevemos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleErroGenerico(Exception ex) {
        // As exceções do próprio Spring MVC (rota inexistente, método não suportado, media
        // type inválido...) implementam ErrorResponse e já carregam o status correto. Sem
        // este desvio elas caíam neste catch-all: uma URL digitada errada respondia 500.
        HttpStatusCode status = ex instanceof ErrorResponse erroSpring
                ? erroSpring.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.is5xxServerError() ? "Erro Interno no Servidor" : "Requisição inválida",
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(erro);
    }

}
