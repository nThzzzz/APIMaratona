package com.APImaratona.Maratona.Exceptions;

import com.APImaratona.Maratona.DTO.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Opcional: Captura qualquer outro erro que não prevemos e retorna 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleErroGenerico(Exception ex) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno no Servidor",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

}
