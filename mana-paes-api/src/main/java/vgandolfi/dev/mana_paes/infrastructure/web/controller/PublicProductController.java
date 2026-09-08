package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.response.ProductResponse;
import vgandolfi.dev.mana_paes.application.service.ProductService;

import java.util.List;

/**
 * Catálogo público de produtos (sem autenticação) para a página {@code /pedido}
 * do vendedor: dado o telefone do link mágico, lista os produtos ATIVOS do
 * tenant do vendedor, com os grupos de opções.
 */
@RestController
@RequestMapping("/api/v1/public/products")
public class PublicProductController {

    private final ProductService productService;

    public PublicProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listActiveByVendorPhone(
            @RequestParam("phone") String vendorPhone) {
        return ResponseEntity.ok(productService.listActivePublic(vendorPhone));
    }
}