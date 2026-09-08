import type { ReactNode } from "react";
import { Link } from "react-router";
import { PageHeader } from "~/components/ui/page-header";
import { useAuth } from "~/hooks/use-auth";

interface HelpSectionProps {
  title: string;
  href?: string;
  hrefLabel?: string;
  children: ReactNode;
}

function HelpSection({ title, href, hrefLabel = "Abrir esta página", children }: HelpSectionProps) {
  return (
    <section className="card border border-base-300 bg-base-100">
      <div className="card-body">
        <h2 className="card-title text-lg">{title}</h2>
        <div className="space-y-3 text-sm leading-relaxed text-base-content/80">
          {children}
        </div>
        {href ? (
          <div className="card-actions mt-2">
            <Link to={href} className="btn btn-outline btn-sm">
              {hrefLabel}
            </Link>
          </div>
        ) : null}
      </div>
    </section>
  );
}

export default function HelpPage() {
  const { user } = useAuth();
  const firstName = user?.name?.trim().split(/\s+/)[0] ?? "Breno";

  return (
    <div className="space-y-6">
      <PageHeader
        title="Como funciona"
        subtitle="Guia rápido do sistema — do pedido do vendedor ao relatório de produção."
      />

      <div className="card border border-base-300 bg-base-100">
        <div className="card-body">
          <h2 className="card-title text-xl">Bem-vindo, {firstName}!</h2>
          <p className="text-sm leading-relaxed text-base-content/80">
            Este sistema centraliza o fluxo de pedidos da padaria: os vendedores
            fazem seus pedidos por um link público, você confere e aceita, e os
            relatórios de produção e financeiro são gerados automaticamente no
            dia. No menu lateral você encontra todas as telas descritas abaixo.
          </p>
          <div className="flex flex-wrap gap-2 text-sm">
            <Link to="/orders" className="btn btn-outline btn-sm">
              Ver pedidos
            </Link>
            <Link to="/reports/production" className="btn btn-outline btn-sm">
              Relatório de produção
            </Link>
            <Link to="/reports/financial" className="btn btn-outline btn-sm">
              Relatório financeiro
            </Link>
            <Link to="/settings/reminders" className="btn btn-outline btn-sm">
              Lembretes
            </Link>
          </div>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <HelpSection title="1. Pedidos" href="/orders">
          <p>
            Os vendedores fazem pedidos por um{" "}
            <span className="font-medium text-base-content">link público</span>,
            sem precisar de login. O telefone informado no link identifica o
            vendedor automaticamente. Cada item do pedido tem quantidade por
            turno: <span className="badge badge-ghost badge-sm">Manhã</span>,{" "}
            <span className="badge badge-ghost badge-sm">Tarde</span> e{" "}
            <span className="badge badge-ghost badge-sm">Noite</span> — assim
            você sabe exatamente quanto produzir em cada período.
          </p>
          <p>
            Todo pedido passa por um status:
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              <span className="badge badge-warning badge-sm">Pendente</span> —
              chegou e aguarda sua confirmação.
            </li>
            <li>
              <span className="badge badge-success badge-sm">Aceito</span> —
              confirmado e entra na produção do dia.
            </li>
            <li>
              <span className="badge badge-error badge-sm">Cancelado</span> —
              não será produzido.
            </li>
          </ul>
          <p>
            Na tela de pedidos você pode aceitar um a um ou usar o botão{" "}
            <span className="font-medium text-base-content">"Aceitar todos"</span>{" "}
            para aceitar todos os pendentes de uma vez.
          </p>
        </HelpSection>

        <HelpSection title="2. Produtos e grupos de opções" href="/products">
          <p>
            Cada produto tem nome, preço e unidade de medida (unidade ou kg). Os{" "}
            <span className="font-medium text-base-content">
              grupos de opções
            </span>{" "}
            criam variações do mesmo produto — por exemplo, o grupo "Tamanho"
            com as opções "Pequeno" (sem acréscimo) e "Grande" (com acréscimo no
            preço).
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              <span className="font-medium text-base-content">Grupo obrigatório:</span>{" "}
              o vendedor precisa escolher uma das opções para fechar o pedido.
            </li>
            <li>
              <span className="font-medium text-base-content">Grupo opcional:</span>{" "}
              o vendedor pode escolher ou deixar em branco.
            </li>
            <li>
              Produtos <span className="font-medium text-base-content">ativos</span>{" "}
              aparecem no catálogo do link público de pedido.
            </li>
          </ul>
        </HelpSection>

        <HelpSection title="3. Vendedores" href="/vendors">
          <p>
            Cadastre cada vendedor com nome e telefone. O{" "}
            <span className="font-medium text-base-content">telefone</span> é a
            "identidade" dele no sistema: é ele que identifica o vendedor quando
            o pedido chega pelo link público.
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              O DDI 55 é adicionado automaticamente — você digita o número
              normalmente, sem se preocupar com o prefixo.
            </li>
            <li>
              O vendedor faz o pedido pelo link{" "}
              <code className="rounded bg-base-200 px-1.5 py-0.5 text-xs">
                /pedido?telefone=...&amp;nome=...&amp;dia=...
              </code>
              , que pode ser enviado pelos lembretes ou compartilhado por você.
            </li>
          </ul>
        </HelpSection>

        <HelpSection title="4. Lembretes" href="/settings/reminders">
          <p>
            Os lembretes são <span className="font-medium text-base-content">mensagens automáticas</span>{" "}
            enviadas aos vendedores ativos com o link de pedido, para eles não
            esquecerem de pedir.
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              Você define o nome, a mensagem e o{" "}
              <span className="font-medium text-base-content">horário</span> de
              envio.
            </li>
            <li>
              O campo <span className="font-medium text-base-content">"dias antes da entrega"</span>{" "}
              define quando enviar: 0 = no próprio dia da entrega, 1 = um dia
              antes, e assim por diante.
            </li>
            <li>
              Pode ser enviado por{" "}
              <span className="font-medium text-base-content">WhatsApp</span>{" "}
              e/ou <span className="font-medium text-base-content">e-mail</span>.
            </li>
            <li>
              O botão <span className="font-medium text-base-content">"Enviar agora"</span>{" "}
              manda o lembrete na hora para todos os vendedores ativos.
            </li>
          </ul>
        </HelpSection>

        <HelpSection title="5. Relatórios" href="/reports/production">
          <p>
            <span className="font-medium text-base-content">
              Relatório de produção:
            </span>{" "}
            consolida as quantidades do dia por produto e turno (Manhã/Tarde/Noite),
            mostra o valor total e lista "Pedido(s) de:" com os vendedores que
            pediram. O botão "Enviar relatório" envia na hora para você.
          </p>
          <p>
            No card <span className="font-medium text-base-content">"Envio automático"</span>{" "}
            você define o horário em que o relatório diário é enviado e pode{" "}
            <span className="font-medium text-base-content">
              ligar ou desligar
            </span>{" "}
            o envio automático pelo toggle "Enviar automaticamente".
          </p>
          <p>
            <span className="font-medium text-base-content">
              Relatório financeiro:
            </span>{" "}
            mostra o faturamento do dia e a quantidade de pedidos.
          </p>
          <div className="flex flex-wrap gap-2">
            <Link to="/reports/production" className="btn btn-outline btn-xs">
              Relatório de produção
            </Link>
            <Link to="/reports/financial" className="btn btn-outline btn-xs">
              Relatório financeiro
            </Link>
          </div>
        </HelpSection>

        <HelpSection title="6. Configurações e WhatsApp" href="/settings/notifications">
          <p>
            <span className="font-medium text-base-content">Configurações:</span>{" "}
            aqui ficam o WhatsApp do administrador (destino das notificações e
            relatórios), o e-mail (usado{" "}
            <span className="font-medium text-base-content">
              apenas para recuperação de senha
            </span>
            ), o horário do relatório diário e os toggles de envio por WhatsApp e
            e-mail.
          </p>
          <p>
            <span className="font-medium text-base-content">WhatsApp:</span>{" "}
            a conexão é feita por{" "}
            <span className="font-medium text-base-content">QR code</span>, lido
            no próprio aplicativo do WhatsApp. Enquanto a conta não estiver
            conectada, as notificações e os envios automáticos ficam suspensos —
            vale conferir esta tela se algum envio não chegar.
          </p>
          <div className="flex flex-wrap gap-2">
            <Link to="/settings/whatsapp" className="btn btn-outline btn-xs">
              Conectar WhatsApp
            </Link>
          </div>
        </HelpSection>
      </div>
    </div>
  );
}